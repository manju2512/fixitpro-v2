package com.fixitpro.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Field is still named "username" for wire compatibility with the existing
 * frontend request shape, but as of the flexible-login feature it accepts
 * username, email, OR phone - see UserRepository.findByUsernameOrEmailOrPhoneWithRole.
 */
public record LoginRequest(
        @NotBlank(message = "Username, email, or phone is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
