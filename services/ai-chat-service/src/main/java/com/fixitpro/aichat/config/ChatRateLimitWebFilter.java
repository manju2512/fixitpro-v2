package com.fixitpro.aichat.config;

import com.fixitpro.aichat.auth.AuthWebFilter;
import com.fixitpro.aichat.auth.AuthenticatedUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Limits how often a single logged-in user can hit /api/chat/message. This
 * exists less to protect our own compute (that's what the JVM memory caps
 * are for) and more to stop one user from burning through the shared Groq
 * free-tier quota and starving every other customer of the chat feature.
 *
 * Must run AFTER AuthWebFilter (needs the resolved AuthenticatedUser to key
 * by user id rather than IP) and after CorsWebFilter (HIGHEST_PRECEDENCE),
 * hence @Order(2) here vs AuthWebFilter's @Order(1).
 *
 * In-memory, single-instance appropriate - see AuthRateLimitFilter in
 * core-service for the same design note on why this isn't Redis-backed yet.
 */
@Component
@Order(2)
public class ChatRateLimitWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimitWebFilter.class);

    private final int capacity;
    private final int refillMinutes;
    private final ObjectMapper objectMapper;

    private final Cache<Long, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(10_000)
            .build();

    public ChatRateLimitWebFilter(
            @Value("${app.rate-limit.chat.capacity:15}") int capacity,
            @Value("${app.rate-limit.chat.refill-minutes:5}") int refillMinutes,
            ObjectMapper objectMapper) {
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/chat/message")) {
            return chain.filter(exchange);
        }

        AuthenticatedUser user = exchange.getAttribute(AuthWebFilter.USER_ATTRIBUTE);
        if (user == null) {
            // AuthWebFilter should have already rejected this with 401, but
            // fail closed rather than NPE if filter order ever changes.
            return chain.filter(exchange);
        }

        Bucket bucket = buckets.get(user.userId(), id -> newBucket());
        boolean allowed = bucket.tryConsume(1);
        log.info("[RATE-LIMIT-DEBUG] user={} configuredCapacity={} availableTokensAfter={} allowed={}",
                user.userId(), capacity, bucket.getAvailableTokens(), allowed);
        if (allowed) {
            return chain.filter(exchange);
        }
        return tooManyRequests(exchange);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    Map.of("message", "You're sending messages too quickly. Please wait a bit and try again."));
        } catch (Exception e) {
            body = "{\"message\":\"Rate limit exceeded.\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
