package com.fixitpro.aichat.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRequest(
        @NotEmpty(message = "messages must not be empty")
        @Valid
        List<ChatMessageDto> messages
) {
}
