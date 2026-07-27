package com.fixitpro.aichat.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Guards /api/chat/message: requires a valid access token, same trust
 * boundary as core-service's own JwtAuthFilter, just without the full
 * Spring Security machinery - this service only needs "who is this and are
 * they real", it delegates all actual authorization to core-service when
 * tools call back into it.
 *
 * @Order(1): must run after CorsWebFilter (HIGHEST_PRECEDENCE) and before
 * ChatRateLimitWebFilter (@Order(2)), which needs the AuthenticatedUser this
 * filter resolves in order to rate-limit per user rather than per IP.
 */
@Component
@Order(1)
public class AuthWebFilter implements WebFilter {

    public static final String USER_ATTRIBUTE = "authenticatedUser";
    public static final String TOKEN_ATTRIBUTE = "rawAccessToken";

    private final JwtVerifier jwtVerifier;
    private final ObjectMapper objectMapper;

    public AuthWebFilter(JwtVerifier jwtVerifier, ObjectMapper objectMapper) {
        this.jwtVerifier = jwtVerifier;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/chat/message")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }
        String token = header.substring(7);

        try {
            AuthenticatedUser user = jwtVerifier.verify(token);
            exchange.getAttributes().put(USER_ATTRIBUTE, user);
            exchange.getAttributes().put(TOKEN_ATTRIBUTE, token);
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of("message", message));
        } catch (Exception e) {
            body = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}