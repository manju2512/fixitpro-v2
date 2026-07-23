package com.fixitpro.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Admin request to set an override for a specific date. When closed=true,
 * openTime/closeTime are ignored (the reservation service blocks bookings
 * on that date entirely). When closed=false, openTime/closeTime are
 * required and represent a non-default business-hours window for that date.
 */
public record BusinessScheduleRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        LocalTime openTime,

        LocalTime closeTime,

        boolean closed
) {}
