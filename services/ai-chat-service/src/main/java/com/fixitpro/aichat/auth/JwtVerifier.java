package com.fixitpro.aichat.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Verifies access tokens issued by core-service's JwtService. Must use the
 * exact same key-derivation scheme (Keys.hmacShaKeyFor over the raw UTF-8
 * secret bytes) or every token will fail signature verification here even
 * though core-service considers it perfectly valid.
 */
@Component
public class JwtVerifier {

    private final SecretKey signingKey;

    public JwtVerifier(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** Returns the authenticated caller, or throws JwtException/IllegalArgumentException if the token is invalid, expired, or not an access token. */
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            throw new JwtException("Not an access token");
        }

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Object rawUserId = claims.get("userId");
        if (username == null || role == null || !(rawUserId instanceof Number userIdNumber)) {
            throw new JwtException("Access token is missing required claims");
        }

        return new AuthenticatedUser(userIdNumber.longValue(), username, role);
    }
}
