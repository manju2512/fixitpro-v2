package com.fixitpro.aichat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChatMessageDto(
        @Pattern(regexp = "user|assistant", message = "role must be 'user' or 'assistant'")
        String role,
        @NotBlank(message = "content is required")
        String content
) {
}
