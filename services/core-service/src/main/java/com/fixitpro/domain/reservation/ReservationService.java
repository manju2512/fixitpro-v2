package com.fixitpro.domain.reservation;

import com.fixitpro.common.exception.InvalidStateTransitionException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.common.exception.UnauthorizedActionException;
import com.fixitpro.domain.reservation.dto.CreateReservationRequest;
import com.fixitpro.domain.reservation.dto.ReservationResponse;
import com.fixitpro.domain.role.RoleName;
import com.fixitpro.domain.schedule.BusinessSchedule;
import com.fixitpro.domain.schedule.BusinessScheduleRepository;
import com.fixitpro.domain.servicetype.ServiceType;
import com.fixitpro.domain.servicetype.ServiceTypeService;
import com.fixitpro.domain.technician.TechnicianProfile;
import com.fixitpro.domain.technician.TechnicianService;
import com.fixitpro.domain.user.User;
import com.fixitpro.domain.user.UserRepository;
import com.fixitpro.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ServiceTypeService serviceTypeService;
    private final TechnicianService technicianService;
    private final BusinessScheduleRepository businessScheduleRepository;

    @Transactional
    public ReservationResponse create(Long customerId, CreateReservationRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        ServiceType serviceType = serviceTypeService.getEntityOrThrow(request.serviceTypeId());

        validateBusinessOpen(request.reservationDate());

        Reservation.ReservationBuilder builder = Reservation.builder()
                .customer(customer)
                .serviceType(serviceType)
                .reservationDate(request.reservationDate())
                .timeSlot(request.timeSlot())
                .address(request.address())
                .telephone(request.telephone())
                .comments(request.comments());

        if (request.technicianId() != null) {
            // Customer chose a specific technician - needs confirmation before
            // it's locked in, so we don't silently double-book someone.
            TechnicianProfile chosen = technicianService.getEntityOrThrow(request.technicianId());
            if (!chosen.getServiceType().getServiceTypeId().equals(serviceType.getServiceTypeId())) {
                throw new InvalidStateTransitionException(
                        "Selected technician does not offer the requested service type");
            }
            if (!chosen.isAvailable()) {
                throw new InvalidStateTransitionException(
                        "Selected technician is currently unavailable - choose another or leave unassigned for auto-assignment");
            }
            builder.technician(chosen).status(ReservationStatus.PENDING);
        } else {
            // No preference given - auto-assign the least-busy available technician.
            Optional<TechnicianProfile> best =
                    technicianService.findBestAvailableTechnician(serviceType.getServiceTypeId(), request.reservationDate());

            if (best.isPresent()) {
                builder.technician(best.get()).status(ReservationStatus.CONFIRMED);
            } else {
                // Nobody available right now - booking still goes through as a
                // request; an admin assigns a technician once one frees up.
                builder.status(ReservationStatus.PENDING);
            }
        }

        Reservation saved = reservationRepository.save(builder.build());
        return ReservationResponse.from(reservationRepository.findByIdWithDetails(saved.getReservationId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long customerId) {
        return reservationRepository.findAllByCustomerId(customerId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyAssignments(Long technicianUserId) {
        TechnicianProfile profile = technicianService.getByUserIdOrThrow(technicianUserId);
        return reservationRepository.findAllByTechnicianId(profile.getTechnicianId()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAll() {
        return reservationRepository.findAllWithDetails().stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getByIdForRequester(Long reservationId, UserPrincipal requester) {
        Reservation reservation = getEntityOrThrow(reservationId);
        assertCanView(reservation, requester);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(Long reservationId, String newStatusRaw, UserPrincipal requester) {
        Reservation reservation = getEntityOrThrow(reservationId);
        ReservationStatus newStatus = parseStatus(newStatusRaw);

        assertCanChangeStatus(reservation, newStatus, requester);

        if (!reservation.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move reservation from %s to %s".formatted(reservation.getStatus(), newStatus));
        }

        reservation.setStatus(newStatus);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse assignTechnician(Long reservationId, Long technicianId) {
        Reservation reservation = getEntityOrThrow(reservationId);
        TechnicianProfile technician = technicianService.getEntityOrThrow(technicianId);

        if (!technician.getServiceType().getServiceTypeId().equals(reservation.getServiceType().getServiceTypeId())) {
            throw new InvalidStateTransitionException("Technician does not offer the reservation's service type");
        }
        if (reservation.getStatus().isTerminal()) {
            throw new InvalidStateTransitionException("Cannot reassign a " + reservation.getStatus() + " reservation");
        }

        reservation.setTechnician(technician);
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }
        return ReservationResponse.from(reservation);
    }

    // --- internal helpers ---

    private Reservation getEntityOrThrow(Long id) {
        return reservationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private ReservationStatus parseStatus(String raw) {
        try {
            return ReservationStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateTransitionException("Unknown status: " + raw);
        }
    }

    private void assertCanView(Reservation reservation, UserPrincipal requester) {
        boolean isOwner = reservation.getCustomer().getUserId().equals(requester.getUserId());
        boolean isAssignedTechnician = reservation.getTechnician() != null
                && reservation.getTechnician().getUser().getUserId().equals(requester.getUserId());
        boolean isAdmin = RoleName.ADMIN.name().equals(requester.getRoleName());

        if (!isOwner && !isAssignedTechnician && !isAdmin) {
            throw new UnauthorizedActionException("You do not have access to this reservation");
        }
    }

    private void assertCanChangeStatus(Reservation reservation, ReservationStatus newStatus, UserPrincipal requester) {
        boolean isAdmin = RoleName.ADMIN.name().equals(requester.getRoleName());
        if (isAdmin) return;

        boolean isOwner = reservation.getCustomer().getUserId().equals(requester.getUserId());
        boolean isAssignedTechnician = reservation.getTechnician() != null
                && reservation.getTechnician().getUser().getUserId().equals(requester.getUserId());

        if (isOwner) {
            // Customers may only cancel - nothing else.
            if (newStatus != ReservationStatus.CANCELLED) {
                throw new UnauthorizedActionException("Customers can only cancel a reservation");
            }
            return;
        }

        if (isAssignedTechnician) {
            // Technicians drive the job forward but can't invent a cancellation
            // for someone else's booking without going through admin/customer.
            if (newStatus == ReservationStatus.CANCELLED) {
                throw new UnauthorizedActionException("Technicians cannot cancel a reservation");
            }
            return;
        }

        throw new UnauthorizedActionException("You do not have permission to change this reservation's status");
    }

    private void validateBusinessOpen(java.time.LocalDate date) {
        Optional<BusinessSchedule> schedule = businessScheduleRepository.findByDate(date);
        // No explicit schedule row = business is open by default (standard hours).
        // An explicit row with closed=true blocks the date.
        schedule.ifPresent(s -> {
            if (s.isClosed()) {
                throw new InvalidStateTransitionException("The business is closed on " + date);
            }
        });
    }
}
