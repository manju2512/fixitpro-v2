package com.fixitpro.domain.review.dto;

import com.fixitpro.domain.review.Review;
import com.fixitpro.domain.review.ReviewReply;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewResponse(
        Long reviewId,
        Long reservationId,
        Long customerId,
        String customerName,
        Long technicianId,
        String technicianName,
        Integer rating,
        String comment,
        boolean edited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ReviewReplyResponse reply
) {
    /**
     * @param reply pass null to omit the reply entirely (e.g. a moderated-away
     *              reply hidden from the public view); the caller decides
     *              visibility rather than this DTO.
     */
    public static ReviewResponse from(Review r, ReviewReply reply) {
        var technician = r.getReservation().getTechnician();
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .reservationId(r.getReservation().getReservationId())
                .customerId(r.getCustomer().getUserId())
                .customerName(r.getCustomer().getUsername())
                .technicianId(technician != null ? technician.getTechnicianId() : null)
                .technicianName(technician != null ? technician.getUser().getUsername() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .edited(r.isEdited())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .reply(reply != null ? ReviewReplyResponse.from(reply) : null)
                .build();
    }
}
