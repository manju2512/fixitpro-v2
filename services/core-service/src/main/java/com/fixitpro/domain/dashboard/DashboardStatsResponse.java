package com.fixitpro.domain.dashboard;

import lombok.Builder;

import java.util.Map;

@Builder
public record DashboardStatsResponse(
        long totalCustomers,
        long totalTechnicians,
        long totalReservations,
        Map<String, Long> reservationsByStatus,
        long totalReviews,
        Double averageRating
) {}
