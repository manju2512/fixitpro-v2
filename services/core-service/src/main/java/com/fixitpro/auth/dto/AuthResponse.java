package com.fixitpro.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String username,
        String role
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long userId, String username, String role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(userId)
                .username(username)
                .role(role)
                .build();
    }
}
