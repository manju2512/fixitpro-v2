package com.fixitpro.domain.technician;

import com.fixitpro.common.exception.DuplicateResourceException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.domain.role.Role;
import com.fixitpro.domain.role.RoleName;
import com.fixitpro.domain.role.RoleRepository;
import com.fixitpro.domain.servicetype.ServiceType;
import com.fixitpro.domain.servicetype.ServiceTypeRepository;
import com.fixitpro.domain.technician.dto.AdminCreateTechnicianRequest;
import com.fixitpro.domain.technician.dto.TechnicianResponse;
import com.fixitpro.domain.user.User;
import com.fixitpro.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TechnicianResponse create(AdminCreateTechnicianRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role technicianRole = roleRepository.findByName(RoleName.TECHNICIAN.name())
                .orElseThrow(() -> new ResourceNotFoundException("TECHNICIAN role not configured"));

        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + request.serviceTypeId()));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(technicianRole)
                .active(true)
                .build();
        userRepository.save(user);

        TechnicianProfile profile = TechnicianProfile.builder()
                .user(user)
                .serviceType(serviceType)
                .bio(request.bio())
                .yearsExperience(request.yearsExperience() != null ? request.yearsExperience() : 0)
                .available(true)
                .build();
        technicianRepository.save(profile);

        return TechnicianResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public List<TechnicianResponse> listAvailableForService(Long serviceTypeId) {
        return technicianRepository.findByServiceType_ServiceTypeIdAndAvailableTrue(serviceTypeId).stream()
                .map(TechnicianResponse::from)
                .toList();
    }

    @Transactional
    public void setAvailability(Long technicianId, boolean available) {
        TechnicianProfile profile = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));
        profile.setAvailable(available);
    }

    @Transactional(readOnly = true)
    public TechnicianProfile getEntityOrThrow(Long id) {
        return technicianRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));
    }

    @Transactional(readOnly = true)
    public TechnicianProfile getByUserIdOrThrow(Long userId) {
        return technicianRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No technician profile for this account"));
    }

    /**
     * Auto-assignment: among technicians available for the given service type,
     * picks the one with the fewest active reservations on the requested date
     * (least-busy-first). Returns empty if no technician is available at all.
     */
    @Transactional(readOnly = true)
    public Optional<TechnicianProfile> findBestAvailableTechnician(Long serviceTypeId, LocalDate date) {
        List<TechnicianProfile> candidates =
                technicianRepository.findByServiceType_ServiceTypeIdAndAvailableTrue(serviceTypeId);

        return candidates.stream()
                .min(Comparator.comparingLong(t ->
                        technicianRepository.countActiveReservationsOnDate(t.getTechnicianId(), date)));
    }
}
