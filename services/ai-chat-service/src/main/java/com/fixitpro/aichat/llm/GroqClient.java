package com.fixitpro.aichat.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Groq's API deliberately mirrors OpenAI's Chat Completions format
 * (https://api.groq.com/openai/v1/chat/completions, Bearer auth) - a long
 * stable, well-documented contract, chosen specifically to minimize
 * integration risk for a provider whose exact API surface can't be verified
 * against the live service from this build environment.
 */
@Component
public class GroqClient {

    private static final int MAX_TOKENS = 1024;

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

        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, tools, MAX_TOKENS);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("(no body)")
                                .map(body -> new GroqApiException(
                                        "Groq API returned " + response.statusCode() + ": " + body)))
                .bodyToMono(ChatCompletionResponse.class);
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
}
