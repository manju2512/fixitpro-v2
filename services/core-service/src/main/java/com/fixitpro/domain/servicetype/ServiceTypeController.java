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

    @DeleteMapping("/api/admin/service-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        serviceTypeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
