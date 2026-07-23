package com.fixitpro.domain.reservation;

import com.fixitpro.domain.reservation.dto.AssignTechnicianRequest;
import com.fixitpro.domain.reservation.dto.CreateReservationRequest;
import com.fixitpro.domain.reservation.dto.ReservationResponse;
import com.fixitpro.domain.reservation.dto.UpdateStatusRequest;
import com.fixitpro.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.create(principal.getUserId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> myReservations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reservationService.getMyReservations(principal.getUserId()));
    }

    @GetMapping("/technicians/me")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<List<ReservationResponse>> myAssignments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reservationService.getMyAssignments(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reservationService.getByIdForRequester(id, principal));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request.status(), principal));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> assignTechnician(
            @PathVariable Long id, @Valid @RequestBody AssignTechnicianRequest request) {
        return ResponseEntity.ok(reservationService.assignTechnician(id, request.technicianId()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }
}
