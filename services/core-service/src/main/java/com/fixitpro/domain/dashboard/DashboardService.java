package com.fixitpro.domain.dashboard;

import com.fixitpro.domain.reservation.ReservationRepository;
import com.fixitpro.domain.reservation.ReservationStatus;
import com.fixitpro.domain.review.ReviewRepository;
import com.fixitpro.domain.role.RoleName;
import com.fixitpro.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            byStatus.put(status.name(), reservationRepository.countByStatus(status));
        }

        return DashboardStatsResponse.builder()
                .totalCustomers(userRepository.countByRole_Name(RoleName.CUSTOMER.name()))
                .totalTechnicians(userRepository.countByRole_Name(RoleName.TECHNICIAN.name()))
                .totalReservations(reservationRepository.count())
                .reservationsByStatus(byStatus)
                .totalReviews(reviewRepository.count())
                .averageRating(reviewRepository.findAverageRating())
                .build();
    }
}
