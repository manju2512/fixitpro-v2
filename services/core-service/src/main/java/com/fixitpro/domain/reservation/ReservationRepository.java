package com.fixitpro.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.serviceType
            LEFT JOIN FETCH r.technician t
            LEFT JOIN FETCH t.user
            WHERE r.customer.userId = :customerId
            ORDER BY r.reservationDate DESC, r.reservationId DESC
            """)
    List<Reservation> findAllByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.serviceType
            JOIN FETCH r.customer
            WHERE r.technician.technicianId = :technicianId
            ORDER BY r.reservationDate DESC, r.reservationId DESC
            """)
    List<Reservation> findAllByTechnicianId(@Param("technicianId") Long technicianId);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.serviceType
            JOIN FETCH r.customer
            LEFT JOIN FETCH r.technician t
            LEFT JOIN FETCH t.user
            WHERE r.reservationId = :id
            """)
    Optional<Reservation> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.serviceType
            JOIN FETCH r.customer
            LEFT JOIN FETCH r.technician t
            LEFT JOIN FETCH t.user
            ORDER BY r.reservationDate DESC, r.reservationId DESC
            """)
    List<Reservation> findAllWithDetails();

    long countByStatus(ReservationStatus status);
}
