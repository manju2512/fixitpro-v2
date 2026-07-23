package com.fixitpro.domain.technician;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<TechnicianProfile, Long> {

    List<TechnicianProfile> findByServiceType_ServiceTypeIdAndAvailableTrue(Long serviceTypeId);

    @Query("SELECT t FROM TechnicianProfile t JOIN FETCH t.user JOIN FETCH t.serviceType WHERE t.technicianId = :id")
    Optional<TechnicianProfile> findByIdWithDetails(@Param("id") Long id);

    Optional<TechnicianProfile> findByUser_UserId(Long userId);

    /** Admin view: every technician regardless of availability, unlike the public listing. */
    @Query("SELECT t FROM TechnicianProfile t JOIN FETCH t.user JOIN FETCH t.serviceType ORDER BY t.technicianId")
    List<TechnicianProfile> findAllWithDetails();

    /**
     * Counts a technician's active (non-terminal) reservations on a given date.
     * Used by the auto-assignment algorithm to pick the least-busy technician
     * rather than naively assigning the first one found.
     */
    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.technician.technicianId = :technicianId
              AND r.reservationDate = :date
              AND r.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')
            """)
    long countActiveReservationsOnDate(@Param("technicianId") Long technicianId, @Param("date") LocalDate date);
}
