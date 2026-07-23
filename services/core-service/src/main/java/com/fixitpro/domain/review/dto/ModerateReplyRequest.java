package com.fixitpro.domain.review.dto;

import jakarta.validation.constraints.NotBlank;

/** status must be one of ReviewReplyStatus: VISIBLE, HIDDEN, DELETED. */
public record ModerateReplyRequest(
        @NotBlank(message = "Status is required")
        String status
) {}
