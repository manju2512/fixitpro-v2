package com.fixitpro.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReplyRequest(
        @NotBlank(message = "Reply text is required")
        @Size(max = 2000, message = "Reply must be under 2000 characters")
        String replyText
) {}
