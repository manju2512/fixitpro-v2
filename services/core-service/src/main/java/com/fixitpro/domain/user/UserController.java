package com.fixitpro.domain.user;

import com.fixitpro.domain.user.dto.ChangePasswordRequest;
import com.fixitpro.domain.user.dto.UserSummaryResponse;
import com.fixitpro.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    /** Any authenticated user changes their own password - closes the gap left by the seeded bootstrap admin. */
    @PatchMapping("/api/users/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> listAll(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.listAll(role));
    }

    @PatchMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.setActive(principal.getUserId(), id, active);
        return ResponseEntity.noContent().build();
    }
}
