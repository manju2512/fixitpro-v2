package com.fixitpro.domain.review.dto;

import com.fixitpro.domain.review.ReviewReply;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewReplyResponse(
        Long replyId,
        Long technicianId,
        String technicianName,
        String replyText,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewReplyResponse from(ReviewReply reply) {
        return ReviewReplyResponse.builder()
                .replyId(reply.getReplyId())
                .technicianId(reply.getTechnician().getTechnicianId())
                .technicianName(reply.getTechnician().getUser().getUsername())
                .replyText(reply.getReplyText())
                .status(reply.getStatus().name())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }
}
