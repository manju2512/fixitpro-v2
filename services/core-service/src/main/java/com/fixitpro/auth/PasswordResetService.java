package com.fixitpro.auth;

import com.fixitpro.common.exception.InvalidStateTransitionException;
import com.fixitpro.domain.user.User;
import com.fixitpro.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Reset tokens live in Redis with a TTL rather than a new DB table/column -
 * they're inherently short-lived, single-use, and don't need to survive a
 * Redis restart (worst case, a user re-requests the email). One less
 * Flyway migration, and expiry is enforced by Redis itself instead of a
 * manually-checked timestamp column.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String REDIS_KEY_PREFIX = "password-reset:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Always succeeds from the caller's point of view, whether or not the
     * email is actually registered - this is deliberate. Returning a
     * different response for "email not found" vs "email found" lets an
     * attacker enumerate registered accounts one guess at a time. The UI
     * always shows the same "if that email exists, we sent a link" message.
     */
    @Transactional(readOnly = true)
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = generateToken();
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + token, user.getUserId().toString(), TOKEN_TTL);
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = REDIS_KEY_PREFIX + token;
        String userIdRaw = redisTemplate.opsForValue().get(key);

        if (userIdRaw == null) {
            throw new InvalidStateTransitionException("This reset link is invalid or has expired - request a new one");
        }

        // One-time use: delete immediately, before touching the password,
        // so a retry after a mid-request failure can't silently reuse it.
        redisTemplate.delete(key);

        Long userId = Long.valueOf(userIdRaw);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidStateTransitionException("This reset link is invalid or has expired - request a new one"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
