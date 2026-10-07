package com.sem.pmiautoevaluacion.selfEvaluation.entity;

import com.sem.pmiautoevaluacion.shared.value.PeriodRecord;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "self_evaluation_period")
public class SelfEvaluationPeriod {
    @Id
    @Column(name = "year")
    private int year;
    @Embedded
    private PeriodRecord period;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Secretary createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SelfEvaluationPeriod() {}

    public SelfEvaluationPeriod(int year, PeriodRecord period, Secretary createdBy, Instant now) {
        this.year = year;
        this.period = period;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void reschedule(PeriodRecord period, Instant now) {
        this.period = period;
        this.updatedAt = now;
    }

    public int getYear() { return year; }
    public PeriodRecord getPeriod() { return period; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
