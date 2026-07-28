package com.fixitpro.domain.servicetype;

import com.fixitpro.domain.servicetype.dto.ServiceTypeRequest;
import com.fixitpro.domain.servicetype.dto.ServiceTypeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Service Types")
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    @GetMapping("/api/service-types")
    public ResponseEntity<List<ServiceTypeResponse>> listActive() {
        return ResponseEntity.ok(serviceTypeService.listActive());
    }

    /** Admin view - includes inactive service types, so a deactivated one can be found again and reactivated. */
    @GetMapping("/api/admin/service-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceTypeResponse>> listAll() {
        return ResponseEntity.ok(serviceTypeService.listAll());
    }

    @PostMapping("/api/admin/service-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTypeResponse> create(@Valid @RequestBody ServiceTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceTypeService.create(request));
    }

    @PutMapping("/api/admin/service-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTypeResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceTypeRequest request) {
        return ResponseEntity.ok(serviceTypeService.update(id, request));
    }

    /** Toggle on/off - covers both deactivating and reactivating through one endpoint, same shape as technician availability. */
    @PatchMapping("/api/admin/service-types/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTypeResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(serviceTypeService.setActive(id, active));
    }

    @DeleteMapping("/api/admin/service-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        serviceTypeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
