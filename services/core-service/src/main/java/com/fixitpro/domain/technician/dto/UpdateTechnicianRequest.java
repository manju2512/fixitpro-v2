package com.fixitpro.domain.technician.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTechnicianRequest(
        @NotNull(message = "Service type ID is required")
        Long serviceTypeId,

        String bio,

        @Min(value = 0, message = "Years of experience cannot be negative")
        Integer yearsExperience
) {}
