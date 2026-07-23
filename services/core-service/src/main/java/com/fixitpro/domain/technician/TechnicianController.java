package com.fixitpro.domain.technician;

import com.fixitpro.domain.technician.dto.AdminCreateTechnicianRequest;
import com.fixitpro.domain.technician.dto.TechnicianResponse;
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

    @PostMapping("/api/admin/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> create(@Valid @RequestBody AdminCreateTechnicianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(technicianService.create(request));
    }

    /** Lets a logged-in technician toggle their own availability (e.g. going on leave). */
    @PatchMapping("/api/technicians/me/availability")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<Void> setMyAvailability(
            @RequestParam boolean available,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long technicianId = technicianService.getByUserIdOrThrow(principal.getUserId()).getTechnicianId();
        technicianService.setAvailability(technicianId, available);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/technicians/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setAvailability(@PathVariable Long id, @RequestParam boolean available) {
        technicianService.setAvailability(id, available);
        return ResponseEntity.noContent().build();
    }
}
