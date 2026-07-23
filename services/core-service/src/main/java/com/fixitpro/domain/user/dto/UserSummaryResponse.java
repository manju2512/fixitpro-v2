package com.fixitpro.domain.user.dto;

import com.fixitpro.domain.user.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserSummaryResponse(
        Long userId,
        String username,
        String email,
        String phone,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(User u) {
        return UserSummaryResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole().getName())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
