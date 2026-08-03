package com.fixitpro.domain.technician.dto;

import jakarta.validation.constraints.Min;

/**
 * Deliberately narrower than UpdateTechnicianRequest - a technician can
 * update their own bio and years of experience, but not their service
 * type (trade). Changing that is an admin action (AdminTechniciansPage),
 * since it affects which customers can even find/book this technician.
 */
public record UpdateOwnProfileRequest(
        String bio,

        @Min(value = 0, message = "Years of experience cannot be negative")
        Integer yearsExperience
) {}
