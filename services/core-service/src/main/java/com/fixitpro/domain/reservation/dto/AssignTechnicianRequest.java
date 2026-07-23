package com.fixitpro.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTechnicianRequest(
        @NotNull(message = "Technician ID is required")
        Long technicianId
) {}
