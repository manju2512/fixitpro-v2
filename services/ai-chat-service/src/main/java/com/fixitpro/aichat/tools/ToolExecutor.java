package com.fixitpro.aichat.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixitpro.aichat.coreservice.CoreServiceClient;
import com.fixitpro.aichat.coreservice.CoreServiceException;
import com.fixitpro.aichat.coreservice.CreateReservationRequest;
import com.fixitpro.aichat.llm.ChatCompletionMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ToolExecutor {

    private final CoreServiceClient coreServiceClient;
    private final ObjectMapper objectMapper;

    public ToolExecutor(CoreServiceClient coreServiceClient, ObjectMapper objectMapper) {
        this.coreServiceClient = coreServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs the named tool and always resolves to a "tool" role message - errors are
     * captured as plain-text error content, not exceptions, so the agentic loop can
     * keep going and let the model explain the problem to the person rather than the
     * whole request failing.
     *
     * argumentsJson is the raw JSON string from the model's tool call (OpenAI-format
     * tool calls encode arguments as a JSON string, not a native object).
     */
    public Mono<ChatCompletionMessage> execute(String token, String toolCallId, String toolName, String argumentsJson) {
        Map<String, Object> input = parseArguments(argumentsJson);

        Mono<?> result = switch (toolName) {
            case "list_service_types" -> coreServiceClient.listServiceTypes(token);
            case "list_technicians" -> coreServiceClient.listTechnicians(token, requireLong(input, "serviceTypeId"));
            case "create_reservation" -> coreServiceClient.createReservation(token, toCreateRequest(input));
            case "list_my_reservations" -> coreServiceClient.myReservations(token);
            case "cancel_reservation" -> coreServiceClient.cancelReservation(token, requireLong(input, "reservationId"));
            default -> Mono.error(new IllegalArgumentException("Unknown tool: " + toolName));
        };

        return result
                .map(value -> ChatCompletionMessage.toolResult(toolCallId, toJson(value)))
                .onErrorResume(e -> Mono.just(ChatCompletionMessage.toolResult(toolCallId, errorMessage(e))));
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse tool arguments: " + e.getMessage());
        }
    }

    private CreateReservationRequest toCreateRequest(Map<String, Object> input) {
        return new CreateReservationRequest(
                requireLong(input, "serviceTypeId"),
                optionalLong(input, "technicianId"),
                (String) input.get("reservationDate"),
                (String) input.get("timeSlot"),
                (String) input.get("address"),
                (String) input.get("telephone"),
                (String) input.get("comments")
        );
    }

    private Long requireLong(Map<String, Object> input, String key) {
        Long value = optionalLong(input, key);
        if (value == null) throw new IllegalArgumentException("Missing required field: " + key);
        return value;
    }

    private Long optionalLong(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String errorMessage(Throwable e) {
        if (e instanceof CoreServiceException) return e.getMessage();
        return "Tool execution failed: " + e.getMessage();
    }
}
