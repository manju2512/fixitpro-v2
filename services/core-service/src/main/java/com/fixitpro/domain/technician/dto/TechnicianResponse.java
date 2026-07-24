package com.fixitpro.domain.technician.dto;

import com.fixitpro.domain.technician.TechnicianProfile;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TechnicianResponse(
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
    public static TechnicianResponse from(TechnicianProfile t) {
        return TechnicianResponse.builder()
                .technicianId(t.getTechnicianId())
                .name(t.getUser().getUsername())
                .serviceTypeId(t.getServiceType().getServiceTypeId())
                .serviceType(t.getServiceType().getName())
                .bio(t.getBio())
                .yearsExperience(t.getYearsExperience())
                .available(t.isAvailable())
                .ratingAvg(t.getRatingAvg())
                .ratingCount(t.getRatingCount())
                .build();
    }
}