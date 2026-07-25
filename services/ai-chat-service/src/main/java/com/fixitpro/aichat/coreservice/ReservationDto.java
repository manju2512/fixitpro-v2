package com.fixitpro.aichat.coreservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationDto(
        Long reservationId,
        Long customerId,
        String customerName,
        Long technicianId,
        String technicianName,
        Long serviceTypeId,
        String serviceTypeName,
        String reservationDate,
        String timeSlot,
        String status,
        String address,
        String telephone,
        String comments
) {
}
