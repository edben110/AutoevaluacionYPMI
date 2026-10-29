package com.sem.pmiautoevaluacion.pmi.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "pmi_follow_up_completion", uniqueConstraints = @UniqueConstraint(columnNames = "pmi_id"))
public class PMIFollowUpCompletion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pmi_id", nullable = false)
    private PMI pmi;
    @Column(name = "review_year", nullable = false)
    private int reviewYear;
    @Column(name = "control_date", nullable = false)
    private LocalDate controlDate;
    @Column(name = "source_fingerprint", nullable = false, length = 64)
    private String sourceFingerprint;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
    protected PMIFollowUpCompletion() {}
    public PMIFollowUpCompletion(PMI pmi) { this.pmi = pmi; }
    public void complete(int reviewYear, LocalDate date, String fingerprint, Instant now) {
        this.reviewYear = reviewYear; this.controlDate = date;
        this.sourceFingerprint = fingerprint; this.completedAt = now;
    }
    public LocalDate getControlDate() { return controlDate; }
    public String getSourceFingerprint() { return sourceFingerprint; }
    public Instant getCompletedAt() { return completedAt; }
}
