package com.fixitpro.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservation_ReservationId(Long reservationId);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.reservation res
            LEFT JOIN FETCH res.technician t
            LEFT JOIN FETCH t.user
            WHERE r.reviewId = :id
            """)
    Optional<Review> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.reservation res
            JOIN FETCH res.technician t
            WHERE t.technicianId = :technicianId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findAllByTechnicianId(@Param("technicianId") Long technicianId);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.reservation res
            LEFT JOIN FETCH res.technician t
            LEFT JOIN FETCH t.user
            ORDER BY r.createdAt DESC
            """)
    List<Review> findAllWithDetails();

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double findAverageRating();
}
