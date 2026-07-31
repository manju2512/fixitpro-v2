package com.fixitpro.auth;

import com.fixitpro.auth.dto.AuthResponse;
import com.fixitpro.auth.dto.LoginRequest;
import com.fixitpro.auth.dto.RefreshRequest;
import com.fixitpro.auth.dto.SignupRequest;
import com.fixitpro.auth.dto.UsernameAvailabilityResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Lets the signup form check availability as the user types, before
     * they submit - avoids the "fill out the whole form, submit, get told
     * the username is taken" round trip. Public (matches /api/auth/**
     * permitAll) since it's needed before the user has any token.
     */
    @GetMapping("/check-username")
    public ResponseEntity<UsernameAvailabilityResponse> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(authService.checkUsernameAvailability(username));
    }
}