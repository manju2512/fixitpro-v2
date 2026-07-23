package com.fixitpro.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessScheduleRepository extends JpaRepository<BusinessSchedule, Long> {
    Optional<BusinessSchedule> findByDate(LocalDate date);
    List<BusinessSchedule> findAllByOrderByDateAsc();
    boolean existsByDate(LocalDate date);
}
