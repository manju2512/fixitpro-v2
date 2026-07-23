package com.fixitpro.domain.user;

import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.common.exception.UnauthorizedActionException;
import com.fixitpro.domain.user.dto.ChangePasswordRequest;
import com.fixitpro.domain.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedActionException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listAll(String roleFilter) {
        List<User> users = (roleFilter == null || roleFilter.isBlank())
                ? userRepository.findAllWithRole()
                : userRepository.findAllByRoleName(roleFilter.toUpperCase());

        return users.stream().map(UserSummaryResponse::from).toList();
    }

    @Transactional
    public void setActive(Long requestingAdminId, Long targetUserId, boolean active) {
        if (!active && requestingAdminId.equals(targetUserId)) {
            throw new UnauthorizedActionException("You cannot deactivate your own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(active);
    }
}
