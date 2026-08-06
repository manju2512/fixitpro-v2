package com.fixitpro.auth;

import com.fixitpro.auth.dto.AuthResponse;
import com.fixitpro.auth.dto.LoginRequest;
import com.fixitpro.auth.dto.RefreshRequest;
import com.fixitpro.auth.dto.SignupRequest;
import com.fixitpro.auth.dto.UsernameAvailabilityResponse;
import com.fixitpro.common.exception.DuplicateResourceException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.domain.role.Role;
import com.fixitpro.domain.role.RoleName;
import com.fixitpro.domain.role.RoleRepository;
import com.fixitpro.domain.user.User;
import com.fixitpro.domain.user.UserRepository;
import com.fixitpro.security.JwtService;
import com.fixitpro.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Public signup always creates a CUSTOMER account.
     * TECHNICIAN and ADMIN accounts are provisioned separately by an admin
     * (see admin module) - never via self-signup, to prevent privilege escalation.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("username", "Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("email", "Email is already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("phone", "Phone number is already registered");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Default role not configured"));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(customerRole)
                .active(true)
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        return issueTokens(principal);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsernameOrEmailOrPhoneWithRole(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        UserPrincipal principal = new UserPrincipal(user);
        return issueTokens(principal);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        String username = jwtService.extractUsername(token);

        if (!"refresh".equals(jwtService.extractTokenType(token)) || !jwtService.isTokenValid(token, username)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        UserPrincipal principal = new UserPrincipal(user);
        return issueTokens(principal);
    }

    /**
     * Read-only check backing the signup form's live "is this taken?"
     * indicator. Deliberately doesn't validate format here (that's the
     * @Pattern on SignupRequest, enforced at actual submit) - this only
     * answers the one question the frontend can't otherwise answer:
     * does a row with this exact username already exist.
     */
    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse checkUsernameAvailability(String username) {
        return new UsernameAvailabilityResponse(username, !userRepository.existsByUsername(username));
    }

    private AuthResponse issueTokens(UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return AuthResponse.of(accessToken, refreshToken, principal.getUserId(), principal.getUsername(), principal.getRoleName());
    }
}
