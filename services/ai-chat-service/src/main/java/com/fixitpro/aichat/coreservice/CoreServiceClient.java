package com.fixitpro.aichat.coreservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Every call here forwards the person's own access token - core-service
 * decides what they're allowed to do, exactly as it would for a direct API
 * call. This service adds no authorization logic of its own; it's a client,
 * not a second gatekeeper.
 */
@Component
public class CoreServiceClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CoreServiceClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.core-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Mono<List<ServiceTypeDto>> listServiceTypes(String token) {
        return authorized(token).get()
                .uri("/api/service-types")
                .retrieve()
                .onStatus(status -> status.isError(), this::toError)
                .bodyToFlux(ServiceTypeDto.class)
                .collectList();
    }

    public Mono<List<TechnicianDto>> listTechnicians(String token, Long serviceTypeId) {
        return authorized(token).get()
                .uri(uriBuilder -> uriBuilder.path("/api/technicians").queryParam("serviceTypeId", serviceTypeId).build())
                .retrieve()
                .onStatus(status -> status.isError(), this::toError)
                .bodyToFlux(TechnicianDto.class)
                .collectList();
    }

    public Mono<ReservationDto> createReservation(String token, CreateReservationRequest request) {
        return authorized(token).post()
                .uri("/api/reservations")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), this::toError)
                .bodyToMono(ReservationDto.class);
    }

    public Mono<List<ReservationDto>> myReservations(String token) {
        return authorized(token).get()
                .uri("/api/reservations/me")
                .retrieve()
                .onStatus(status -> status.isError(), this::toError)
                .bodyToFlux(ReservationDto.class)
                .collectList();
    }

    public Mono<ReservationDto> cancelReservation(String token, Long reservationId) {
        return authorized(token).patch()
                .uri("/api/reservations/{id}/status", reservationId)
                .bodyValue(new UpdateStatusRequest("CANCELLED"))
                .retrieve()
                .onStatus(status -> status.isError(), this::toError)
                .bodyToMono(ReservationDto.class);
    }

    private WebClient authorized(String token) {
        return webClient.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    private Mono<? extends Throwable> toError(org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new CoreServiceException(extractMessage(body, response.statusCode().value())));
    }

    private String extractMessage(String body, int statusCode) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
            // body wasn't the expected {"message": "..."} shape - fall through
        }
        return "core-service returned " + statusCode + (body.isBlank() ? "" : ": " + body);
    }

    private record UpdateStatusRequest(String status) {
    }
}
