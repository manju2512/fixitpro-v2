package com.fixitpro.aichat.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<ChatCompletionMessage> messages,
        List<ToolSpec> tools,
        @JsonProperty("max_tokens") int maxTokens
) {
}
