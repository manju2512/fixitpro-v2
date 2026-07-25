package com.fixitpro.aichat.coreservice;

import com.fasterxml.jackson.annotation.JsonInclude;

/** reservationDate must be "YYYY-MM-DD" and not in the past - core-service enforces @FutureOrPresent. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateReservationRequest(
        Long serviceTypeId,
        Long technicianId,
        String reservationDate,
        String timeSlot,
        String address,
        String telephone,
        String comments
) {
}
