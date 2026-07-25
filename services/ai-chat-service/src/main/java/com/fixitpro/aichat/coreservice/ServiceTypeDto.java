package com.fixitpro.aichat.coreservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceTypeDto(Long serviceTypeId, String name, String description, BigDecimal basePrice) {
}
