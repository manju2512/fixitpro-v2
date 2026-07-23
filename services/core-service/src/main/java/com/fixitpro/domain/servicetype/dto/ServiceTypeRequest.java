package com.fixitpro.domain.servicetype.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServiceTypeRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Base price cannot be negative")
        BigDecimal basePrice
) {}
