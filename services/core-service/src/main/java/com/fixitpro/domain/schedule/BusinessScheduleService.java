package com.fixitpro.domain.schedule;

import com.fixitpro.common.exception.DuplicateResourceException;
import com.fixitpro.common.exception.InvalidStateTransitionException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.domain.schedule.dto.BusinessScheduleRequest;
import com.fixitpro.domain.schedule.dto.BusinessScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessScheduleService {

    private final BusinessScheduleRepository businessScheduleRepository;

    @Transactional(readOnly = true)
    public List<BusinessScheduleResponse> listAll() {
        return businessScheduleRepository.findAllByOrderByDateAsc().stream()
                .map(BusinessScheduleResponse::from)
                .toList();
    }

    /**
     * Public lookup for a single date. No explicit row = business is open
     * with default hours (matches ReservationService.validateBusinessOpen).
     */
    @Transactional(readOnly = true)
    public BusinessScheduleResponse getByDate(java.time.LocalDate date) {
        return businessScheduleRepository.findByDate(date)
                .map(BusinessScheduleResponse::from)
                .orElseGet(() -> BusinessScheduleResponse.builder()
                        .scheduleId(null)
                        .date(date)
                        .openTime(null)
                        .closeTime(null)
                        .closed(false)
                        .build());
    }

    @Transactional
    public BusinessScheduleResponse create(BusinessScheduleRequest request) {
        if (businessScheduleRepository.existsByDate(request.date())) {
            throw new DuplicateResourceException("A schedule override already exists for " + request.date());
        }
        validateTimes(request);

        BusinessSchedule schedule = BusinessSchedule.builder()
                .date(request.date())
                .openTime(request.closed() ? defaultTime(request.openTime()) : request.openTime())
                .closeTime(request.closed() ? defaultTime(request.closeTime()) : request.closeTime())
                .closed(request.closed())
                .build();

        return BusinessScheduleResponse.from(businessScheduleRepository.save(schedule));
    }

    @Transactional
    public BusinessScheduleResponse update(Long scheduleId, BusinessScheduleRequest request) {
        BusinessSchedule schedule = getEntityOrThrow(scheduleId);
        validateTimes(request);

        schedule.setDate(request.date());
        schedule.setClosed(request.closed());
        schedule.setOpenTime(request.closed() ? defaultTime(request.openTime()) : request.openTime());
        schedule.setCloseTime(request.closed() ? defaultTime(request.closeTime()) : request.closeTime());

        return BusinessScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long scheduleId) {
        BusinessSchedule schedule = getEntityOrThrow(scheduleId);
        businessScheduleRepository.delete(schedule);
    }

    private BusinessSchedule getEntityOrThrow(Long id) {
        return businessScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business schedule entry not found: " + id));
    }

    private void validateTimes(BusinessScheduleRequest request) {
        if (!request.closed()) {
            if (request.openTime() == null || request.closeTime() == null) {
                throw new InvalidStateTransitionException("openTime and closeTime are required when the date is not closed");
            }
            if (!request.openTime().isBefore(request.closeTime())) {
                throw new InvalidStateTransitionException("openTime must be before closeTime");
            }
        }
    }

    private LocalTime defaultTime(LocalTime provided) {
        return provided != null ? provided : LocalTime.MIDNIGHT;
    }
}
