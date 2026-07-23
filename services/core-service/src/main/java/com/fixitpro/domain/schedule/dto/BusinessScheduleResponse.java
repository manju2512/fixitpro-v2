package com.fixitpro.domain.schedule.dto;

import com.fixitpro.domain.schedule.BusinessSchedule;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record BusinessScheduleResponse(
        Long scheduleId,
        LocalDate date,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
    public static BusinessScheduleResponse from(BusinessSchedule s) {
        return BusinessScheduleResponse.builder()
                .scheduleId(s.getScheduleId())
                .date(s.getDate())
                .openTime(s.getOpenTime())
                .closeTime(s.getCloseTime())
                .closed(s.isClosed())
                .build();
    }
}
