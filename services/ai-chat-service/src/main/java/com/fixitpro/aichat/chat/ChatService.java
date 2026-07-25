package com.fixitpro.aichat.chat;

import com.fixitpro.aichat.auth.AuthenticatedUser;
import com.fixitpro.aichat.llm.ChatCompletionMessage;
import com.fixitpro.aichat.llm.ChatCompletionResponse;
import com.fixitpro.aichat.llm.GroqClient;
import com.fixitpro.aichat.llm.ToolCall;
import com.fixitpro.aichat.llm.ToolSpec;
import com.fixitpro.aichat.tools.ToolDefinitions;
import com.fixitpro.aichat.tools.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Guards against a runaway tool-use loop (e.g. the model repeatedly calling a tool without ever concluding) burning API quota indefinitely. */
    private static final int MAX_TOOL_ITERATIONS = 6;

    private final GroqClient groqClient;
    private final ToolExecutor toolExecutor;

    public ChatService(GroqClient groqClient, ToolExecutor toolExecutor) {
        this.groqClient = groqClient;
        this.toolExecutor = toolExecutor;
    }

    public Mono<ChatResponse> handle(AuthenticatedUser user, String token, ChatRequest request) {
        List<ChatCompletionMessage> messages = new ArrayList<>();
        messages.add(ChatCompletionMessage.system(buildSystemPrompt(user)));
        for (ChatMessageDto m : request.messages()) {
            messages.add("assistant".equals(m.role())
                    ? ChatCompletionMessage.assistant(m.content())
                    : ChatCompletionMessage.user(m.content()));
        }

        List<ToolSpec> tools = ToolDefinitions.all();

        return runLoop(messages, tools, token, 0)
                .map(reply -> {
                    List<ChatMessageDto> updatedHistory = new ArrayList<>(request.messages());
                    updatedHistory.add(new ChatMessageDto("assistant", reply));
                    return new ChatResponse(reply, updatedHistory);
                })
                .onErrorResume(GroqClient.GroqNotConfiguredException.class, e ->
                        Mono.just(new ChatResponse(
                                "The AI assistant isn't set up yet - an admin needs to configure GROQ_API_KEY.",
                                request.messages())))
                .onErrorResume(e -> {
                    log.error("Chat request failed for user {}: {}", user.username(), e.toString(), e);
                    return Mono.just(new ChatResponse(
                            "Sorry, something went wrong on my end. Please try again in a moment.",
                            request.messages()));
                });
    }

    private Mono<String> runLoop(List<ChatCompletionMessage> messages, List<ToolSpec> tools, String token, int depth) {
        if (depth >= MAX_TOOL_ITERATIONS) {
            return Mono.just("I'm having trouble completing that right now - could you try rephrasing, or breaking it into a simpler request?");
        }

        return groqClient.sendMessage(messages, tools)
                .flatMap(response -> {
                    ChatCompletionMessage assistantMessage = response.firstMessage();

                    if (!ChatCompletionResponse.FINISH_TOOL_CALLS.equals(response.finishReason())
                            || assistantMessage.toolCalls() == null
                            || assistantMessage.toolCalls().isEmpty()) {
                        String content = assistantMessage.content();
                        return Mono.just(content != null && !content.isBlank()
                                ? content
                                : "Sorry, I wasn't able to come up with a reply to that - could you rephrase?");
                    }

                    List<ChatCompletionMessage> withAssistantTurn = new ArrayList<>(messages);
                    withAssistantTurn.add(ChatCompletionMessage.assistantWithToolCalls(assistantMessage.toolCalls()));

                    List<ToolCall> toolCalls = assistantMessage.toolCalls();

                    return Flux.fromIterable(toolCalls)
                            .concatMap(call -> toolExecutor.execute(
                                    token, call.id(), call.function().name(), call.function().arguments()))
                            .collectList()
                            .flatMap(toolResultMessages -> {
                                List<ChatCompletionMessage> withToolResults = new ArrayList<>(withAssistantTurn);
                                withToolResults.addAll(toolResultMessages);
                                return runLoop(withToolResults, tools, token, depth + 1);
                            });
                });
    }

    private String buildSystemPrompt(AuthenticatedUser user) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        return """
                You are FixitPro's booking assistant. You're chatting with %s (role: %s). Today is %s.

                FixitPro connects customers with technicians (electricians, plumbers, carpenters) for home repairs.
                Help the customer find the right service, pick or auto-assign a technician, and book an appointment
                through natural conversation - or check on / cancel an existing booking.

                Rules:
                - Always use the provided tools to look up real service types, technicians, and reservations.
                  Never invent IDs, prices, availability, or booking statuses.
                - Before calling create_reservation, confirm the service type, date, time slot, address, and phone
                  number with the customer in conversation - don't guess or assume any of these.
                - If a tool call fails, explain the problem in plain language and suggest what to try instead;
                  don't just repeat the raw error.
                - Keep replies concise and friendly - this is a chat widget, not an email.
                - If asked about something unrelated to FixitPro bookings, briefly redirect back to how you can help
                  with booking or managing a repair.
                """.formatted(user.username(), user.role(), today);
    }
}
