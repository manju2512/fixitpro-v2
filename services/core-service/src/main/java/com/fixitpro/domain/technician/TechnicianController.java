package com.fixitpro.domain.technician;

import com.fixitpro.domain.technician.dto.AdminCreateTechnicianRequest;
import com.fixitpro.domain.technician.dto.TechnicianResponse;
import com.fixitpro.domain.technician.dto.UpdateTechnicianRequest;
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
@RequiredArgsConstructor
@Tag(name = "Technicians")
public class TechnicianController {

    private final TechnicianService technicianService;

    /** Public: lets a customer see available technicians for a service type before booking. */
    @GetMapping("/api/technicians")
    public ResponseEntity<List<TechnicianResponse>> listAvailable(@RequestParam Long serviceTypeId) {
        return ResponseEntity.ok(technicianService.listAvailableForService(serviceTypeId));
    }

    /** Public: single technician detail, e.g. for a profile page. */
    @GetMapping("/api/technicians/{id}")
    public ResponseEntity<TechnicianResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(technicianService.getByIdPublic(id));
    }

    @PostMapping("/api/admin/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> create(@Valid @RequestBody AdminCreateTechnicianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(technicianService.create(request));
    }

    /** Admin: every technician regardless of availability - the public /api/technicians listing filters to available-only. */
    @GetMapping("/api/admin/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> listAllForAdmin() {
        return ResponseEntity.ok(technicianService.listAllForAdmin());
    }

    @PutMapping("/api/admin/technicians/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateTechnicianRequest request) {
        return ResponseEntity.ok(technicianService.update(id, request));
    }

    /** Lets a logged-in technician see their own profile, including current availability. */
    @GetMapping("/api/technicians/me")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<TechnicianResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(TechnicianResponse.from(technicianService.getByUserIdOrThrow(principal.getUserId())));
    }

    /** Lets a logged-in technician toggle their own availability (e.g. going on leave). */
    @PatchMapping("/api/technicians/me/availability")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<TechnicianResponse> setMyAvailability(
            @RequestParam boolean available,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long technicianId = technicianService.getByUserIdOrThrow(principal.getUserId()).getTechnicianId();
        technicianService.setAvailability(technicianId, available);
        return ResponseEntity.ok(TechnicianResponse.from(technicianService.getEntityOrThrow(technicianId)));
    }

    @PatchMapping("/api/admin/technicians/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setAvailability(@PathVariable Long id, @RequestParam boolean available) {
        technicianService.setAvailability(id, available);
        return ResponseEntity.noContent().build();
    }
}