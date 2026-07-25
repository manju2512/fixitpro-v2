package com.fixitpro.aichat.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groq's API deliberately mirrors OpenAI's Chat Completions format
 * (https://api.groq.com/openai/v1/chat/completions, Bearer auth) - a long
 * stable, well-documented contract, chosen specifically to minimize
 * integration risk for a provider whose exact API surface can't be verified
 * against the live service from this build environment.
 */
@Component
public class GroqClient {

    /**
     * openai/gpt-oss-* models on Groq spend part of this budget on an internal "reasoning"
     * channel before ever emitting a tool call or reply - pinning reasoning_effort=low
     * (supported by gpt-oss-20b/120b on Groq) keeps replies snappy for a chat-widget use
     * case and reduces token spend, which also helps stay under the per-minute rate limit
     * on the free/on_demand tier (see MAX_RATE_LIMIT_RETRIES below).
     */
    private static final int MAX_TOKENS = 1024;
    private static final String REASONING_EFFORT = "low";

    /**
     * The free/on_demand Groq tier enforces a tokens-per-minute cap (8000 TPM as of this
     * writing) shared across a whole minute - a short back-to-back conversation (several
     * chat turns, each resending the growing history + tool schema) can hit that cap well
     * within normal use, returning 429 with an exact "try again in N.NNNs" wait. That's a
     * transient, expected condition on this tier, not a real failure - so retry instead of
     * immediately giving up. Capped at 3 attempts so a persistently exhausted quota (e.g.
     * Requested tokens alone exceeding what a wait would free up) still fails fast instead
     * of hanging the request indefinitely.
     */
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile("try again in ([0-9.]+)s");

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GroqClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.groq-api-key}") String apiKey,
            @Value("${app.ai.model}") String model) {
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Mono<ChatCompletionResponse> sendMessage(List<ChatCompletionMessage> messages, List<ToolSpec> tools) {
        if (!isConfigured()) {
            return Mono.error(new GroqNotConfiguredException());
        }

        // reasoning_effort is only accepted by the gpt-oss family - sending it to any other
        // model would be an unrecognized field, so only include it when it actually applies.
        String reasoningEffort = model.contains("gpt-oss") ? REASONING_EFFORT : null;
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, tools, MAX_TOKENS, reasoningEffort);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("(no body)")
                                .map(RateLimitException::new))
                .onStatus(status -> status.isError() && status.value() != 429, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("(no body)")
                                .map(body -> new GroqApiException(
                                        "Groq API returned " + response.statusCode() + ": " + body)))
                .bodyToMono(ChatCompletionResponse.class)
                .retryWhen(Retry.from(signals -> signals.flatMap(signal -> {
                    Throwable failure = signal.failure();
                    if (!(failure instanceof RateLimitException rle) || signal.totalRetries() >= MAX_RATE_LIMIT_RETRIES) {
                        return Mono.error(failure);
                    }
                    // Add a small buffer on top of Groq's stated wait so we don't clock back in
                    // right at the edge of the window and get rate-limited again immediately.
                    return Mono.delay(rle.retryAfter().plusMillis(250));
                })));
    }

    public static class GroqApiException extends RuntimeException {
        public GroqApiException(String message) {
            super(message);
        }
    }

    public static class GroqNotConfiguredException extends RuntimeException {
        public GroqNotConfiguredException() {
            super("GROQ_API_KEY is not set - the AI assistant isn't configured yet.");
        }
    }

    /** Groq's 429 body includes the exact wait time (e.g. "Please try again in 8.115s") - parsed here so the retry can honor it. */
    public static class RateLimitException extends RuntimeException {
        private final Duration retryAfter;

        public RateLimitException(String body) {
            super("Groq API rate limit hit: " + body);
            this.retryAfter = parseRetryAfter(body);
        }

        public Duration retryAfter() {
            return retryAfter;
        }

        private static Duration parseRetryAfter(String body) {
            Matcher matcher = RETRY_AFTER_PATTERN.matcher(body);
            if (matcher.find()) {
                double seconds = Double.parseDouble(matcher.group(1));
                return Duration.ofMillis((long) (seconds * 1000));
            }
            // Fallback if Groq ever changes the message format and we can't parse it.
            return Duration.ofSeconds(10);
        }
    }
}