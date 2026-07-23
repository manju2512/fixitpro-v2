package com.fixitpro.domain.schedule;

import com.fixitpro.domain.schedule.dto.BusinessScheduleRequest;
import com.fixitpro.domain.schedule.dto.BusinessScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Business Schedule")
public class BusinessScheduleController {

    private final BusinessScheduleService businessScheduleService;

    /** Public: lets the frontend show closures/hours before letting a customer pick a date. */
    @GetMapping("/api/business-schedule")
    public ResponseEntity<List<BusinessScheduleResponse>> listAll() {
        return ResponseEntity.ok(businessScheduleService.listAll());
    }

    @GetMapping("/api/business-schedule/{date}")
    public ResponseEntity<BusinessScheduleResponse> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(businessScheduleService.getByDate(date));
    }

    @PostMapping("/api/admin/business-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessScheduleResponse> create(@Valid @RequestBody BusinessScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessScheduleService.create(request));
    }

    @PutMapping("/api/admin/business-schedule/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessScheduleResponse> update(
            @PathVariable Long id, @Valid @RequestBody BusinessScheduleRequest request) {
        return ResponseEntity.ok(businessScheduleService.update(id, request));
    }

    @DeleteMapping("/api/admin/business-schedule/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        businessScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
