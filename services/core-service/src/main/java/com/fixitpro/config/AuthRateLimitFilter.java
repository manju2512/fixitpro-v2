package com.fixitpro.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Protects /api/auth/login and /api/auth/signup from brute-force and spam
 * abuse. In-memory (Caffeine-backed) token bucket keyed by client IP - this
 * is intentionally NOT Redis-backed: with a single instance (current
 * free-tier deployment), a distributed store buys nothing but latency. If
 * this ever runs behind a load balancer with multiple instances, swap the
 * Caffeine cache for a Redis-backed Bucket4j proxy manager - the
 * doFilterInternal logic below wouldn't need to change, only newBucket()'s
 * storage.
 *
 * Limits are env-configurable (see application.yml: app.rate-limit.auth.*)
 * so they can be loosened without a redeploy once there's real traffic to
 * tune against.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/signup");

    private final int capacity;
    private final int refillMinutes;

    // Bounded by expireAfterAccess so abandoned IP entries (bots, scanners
    // hitting many distinct addresses) don't grow this map forever.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public AuthRateLimitFilter(
            @Value("${app.rate-limit.auth.capacity:5}") int capacity,
            @Value("${app.rate-limit.auth.refill-minutes:1}") int refillMinutes) {
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
        System.out.println("[RATE-LIMIT-DEBUG] AuthRateLimitFilter constructed with capacity=" + capacity + " refillMinutes=" + refillMinutes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!LIMITED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ':' + request.getRequestURI();
        Bucket bucket = buckets.get(key, k -> newBucket());

        boolean allowed = bucket.tryConsume(1);
        System.out.println("[RATE-LIMIT-DEBUG] method=" + request.getMethod() + " uri=" + request.getRequestURI()
                + " key=" + key + " capacityField=" + capacity
                + " availableTokensAfter=" + bucket.getAvailableTokens() + " allowed=" + allowed);

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Too many attempts. Please wait a minute and try again.\"}");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // first hop = original client, behind Render's proxy
        }
        return request.getRemoteAddr();
    }
}
