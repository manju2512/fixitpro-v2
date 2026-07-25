package com.fixitpro.aichat.auth;

/** The identity of whoever is chatting, decoded from a core-service-issued access token. */
public record AuthenticatedUser(Long userId, String username, String role) {
}
