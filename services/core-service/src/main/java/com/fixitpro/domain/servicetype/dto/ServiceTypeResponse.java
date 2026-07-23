package com.fixitpro.domain.servicetype.dto;

import com.fixitpro.domain.servicetype.ServiceType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ServiceTypeResponse(
        Long serviceTypeId,
        String name,
        String description,
        BigDecimal basePrice
) {
    public static ServiceTypeResponse from(ServiceType s) {
        return ServiceTypeResponse.builder()
                .serviceTypeId(s.getServiceTypeId())
                .name(s.getName())
                .description(s.getDescription())
                .basePrice(s.getBasePrice())
                .build();
    }
}
