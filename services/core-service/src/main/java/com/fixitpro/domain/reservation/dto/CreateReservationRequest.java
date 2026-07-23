package com.fixitpro.domain.reservation.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull(message = "Service type is required")
        Long serviceTypeId,

        /** Optional - if omitted, the system auto-assigns the least-busy available technician. */
        Long technicianId,

        @NotNull(message = "Reservation date is required")
        @FutureOrPresent(message = "Reservation date cannot be in the past")
        LocalDate reservationDate,

        @NotBlank(message = "Time slot is required")
        String timeSlot,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "Telephone is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Telephone must be valid")
        String telephone,

        String comments
) {}
