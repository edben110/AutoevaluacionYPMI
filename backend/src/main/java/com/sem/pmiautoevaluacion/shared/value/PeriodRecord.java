package com.sem.pmiautoevaluacion.shared.value;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Embeddable
public class PeriodRecord {
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "finish_date", nullable = false)
    private LocalDate finishDate;

    protected PeriodRecord() {}

    public PeriodRecord(LocalDate startDate, LocalDate finishDate) {
        if (startDate == null || finishDate == null) {
            throw new BadRequestException("Debes indicar las fechas de inicio y fin");
        }
        if (finishDate.isBefore(startDate)) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        this.startDate = startDate;
        this.finishDate = finishDate;
    }

    public boolean include(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(finishDate);
    }

    public long daysDuration() { return ChronoUnit.DAYS.between(startDate, finishDate) + 1; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }
}
