package com.fixitpro.aichat.coreservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TechnicianDto(
        Long technicianId,
        String name,
        Long serviceTypeId,
        String serviceType,
        String bio,
        Integer yearsExperience,
        boolean available,
        BigDecimal ratingAvg,
        Integer ratingCount
) {
}
