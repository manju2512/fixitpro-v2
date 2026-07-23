package com.fixitpro.domain.reservation.dto;

import com.fixitpro.domain.reservation.Reservation;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ReservationResponse(
        Long reservationId,
        Long customerId,
        String customerName,
        Long technicianId,
        String technicianName,
        Long serviceTypeId,
        String serviceTypeName,
        LocalDate reservationDate,
        String timeSlot,
        String status,
        String address,
        String telephone,
        String comments,
        LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return ReservationResponse.builder()
                .reservationId(r.getReservationId())
                .customerId(r.getCustomer().getUserId())
                .customerName(r.getCustomer().getUsername())
                .technicianId(r.getTechnician() != null ? r.getTechnician().getTechnicianId() : null)
                .technicianName(r.getTechnician() != null ? r.getTechnician().getUser().getUsername() : null)
                .serviceTypeId(r.getServiceType().getServiceTypeId())
                .serviceTypeName(r.getServiceType().getName())
                .reservationDate(r.getReservationDate())
                .timeSlot(r.getTimeSlot())
                .status(r.getStatus().name())
                .address(r.getAddress())
                .telephone(r.getTelephone())
                .comments(r.getComments())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
