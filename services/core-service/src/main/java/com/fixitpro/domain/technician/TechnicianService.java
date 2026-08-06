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
import com.fixitpro.domain.technician.dto.UpdateOwnProfileRequest;
import com.fixitpro.domain.technician.dto.UpdateTechnicianRequest;
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
            throw new DuplicateResourceException("username", "Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("email", "Email is already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("phone", "Phone number is already registered");
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

    /** Admin view: every technician regardless of availability - the public listing filters to available-only. */
    @Transactional(readOnly = true)
    public List<TechnicianResponse> listAllForAdmin() {
        return technicianRepository.findAllWithDetails().stream()
                .map(TechnicianResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TechnicianResponse getByIdPublic(Long id) {
        return TechnicianResponse.from(getEntityOrThrow(id));
    }

    @Transactional
    public TechnicianResponse update(Long id, UpdateTechnicianRequest request) {
        TechnicianProfile profile = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));

        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + request.serviceTypeId()));

        profile.setServiceType(serviceType);
        profile.setBio(request.bio());
        profile.setYearsExperience(request.yearsExperience() != null ? request.yearsExperience() : profile.getYearsExperience());

        return TechnicianResponse.from(technicianRepository.findByIdWithDetails(id).orElseThrow());
    }

    /** Self-service: bio and experience only - see UpdateOwnProfileRequest for why serviceType isn't editable here. */
    @Transactional
    public TechnicianResponse updateOwnProfile(Long userId, UpdateOwnProfileRequest request) {
        TechnicianProfile profile = getByUserIdOrThrow(userId);

        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.yearsExperience() != null) {
            profile.setYearsExperience(request.yearsExperience());
        }

        return TechnicianResponse.from(technicianRepository.findByIdWithDetails(profile.getTechnicianId()).orElseThrow());
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
        return technicianRepository.findByUserIdWithDetails(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No technician profile for this account"));
    }

    /**
     * Recomputes a technician's running rating average when a review is
     * created or edited. Pass oldRating=null for a brand-new review (bumps
     * ratingCount); pass the previous rating for an edit (count unchanged,
     * average re-weighted).
     */
    @Transactional
    public void recordReviewRating(Long technicianId, Integer oldRating, int newRating) {
        TechnicianProfile profile = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));

        java.math.BigDecimal currentAvg = profile.getRatingAvg();
        int currentCount = profile.getRatingCount();

        java.math.BigDecimal newAvg;
        int newCount;
        if (oldRating == null) {
            newCount = currentCount + 1;
            java.math.BigDecimal total = currentAvg.multiply(java.math.BigDecimal.valueOf(currentCount))
                    .add(java.math.BigDecimal.valueOf(newRating));
            newAvg = total.divide(java.math.BigDecimal.valueOf(newCount), 2, java.math.RoundingMode.HALF_UP);
        } else {
            newCount = currentCount;
            java.math.BigDecimal total = currentAvg.multiply(java.math.BigDecimal.valueOf(currentCount))
                    .subtract(java.math.BigDecimal.valueOf(oldRating))
                    .add(java.math.BigDecimal.valueOf(newRating));
            newAvg = newCount == 0 ? java.math.BigDecimal.ZERO
                    : total.divide(java.math.BigDecimal.valueOf(newCount), 2, java.math.RoundingMode.HALF_UP);
        }

        profile.setRatingAvg(newAvg);
        profile.setRatingCount(newCount);
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
