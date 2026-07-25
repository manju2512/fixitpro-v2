package com.fixitpro.aichat.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {

    public static final String FINISH_TOOL_CALLS = "tool_calls";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(ChatCompletionMessage message, @JsonProperty("finish_reason") String finishReason) {
    }

    /** The first choice's message - this service only ever requests one completion per call. */
    public ChatCompletionMessage firstMessage() {
        return choices.get(0).message();
    }

    public String finishReason() {
        return choices.get(0).finishReason();
    }
}
