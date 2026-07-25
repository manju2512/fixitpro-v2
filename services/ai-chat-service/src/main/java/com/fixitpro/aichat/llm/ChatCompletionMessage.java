package com.fixitpro.aichat.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Covers all four roles the OpenAI-compatible chat completions format uses:
 *  - "system": content only
 *  - "user": content only
 *  - "assistant": content (nullable when it's a pure tool-call turn) + toolCalls
 *  - "tool": content (the result) + toolCallId (which call this answers)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {
    public static ChatCompletionMessage system(String content) {
        return new ChatCompletionMessage("system", content, null, null);
    }

    public static ChatCompletionMessage user(String content) {
        return new ChatCompletionMessage("user", content, null, null);
    }

    public static ChatCompletionMessage assistant(String content) {
        return new ChatCompletionMessage("assistant", content, null, null);
    }

    public static ChatCompletionMessage assistantWithToolCalls(List<ToolCall> toolCalls) {
        return new ChatCompletionMessage("assistant", null, toolCalls, null);
    }

    public static ChatCompletionMessage toolResult(String toolCallId, String content) {
        return new ChatCompletionMessage("tool", content, null, toolCallId);
    }
}
