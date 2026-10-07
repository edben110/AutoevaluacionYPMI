package com.sem.pmiautoevaluacion.selfEvaluation.dto;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationPeriod;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public record SelfEvaluationPeriodResponse(int year, LocalDate startDate, LocalDate finishDate,
        String status, boolean writable, LocalDate today, String timeZone,
        Instant createdAt, Instant updatedAt) {
    public static SelfEvaluationPeriodResponse from(SelfEvaluationPeriod entity, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        var period = entity.getPeriod();
        String status = today.isBefore(period.getStartDate()) ? "SCHEDULED"
                : period.include(today) ? "OPEN" : "CLOSED";
        return new SelfEvaluationPeriodResponse(entity.getYear(), period.getStartDate(),
                period.getFinishDate(), status, period.include(today), today, clock.getZone().getId(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
