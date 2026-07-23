package com.fixitpro.domain.technician.dto;

import jakarta.validation.constraints.*;

/**
 * Admin-only: provisions a new TECHNICIAN user account plus its profile in
 * one step. Technicians never self-signup (see AuthService.signup, which
 * only ever creates CUSTOMER accounts) - this is the sole entry point for
 * technician accounts, closing off privilege escalation via public signup.
 */
public record AdminCreateTechnicianRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Temporary password is required")
        @Size(min = 8, max = 100)
        String password,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid")
        String phone,

        @NotNull(message = "Service type ID is required")
        Long serviceTypeId,

        String bio,

        @Min(value = 0, message = "Years of experience cannot be negative")
        Integer yearsExperience
) {}
