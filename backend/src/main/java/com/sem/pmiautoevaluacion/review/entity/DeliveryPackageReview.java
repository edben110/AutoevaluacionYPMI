package com.sem.pmiautoevaluacion.review.entity;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_package_review", uniqueConstraints = @UniqueConstraint(columnNames = "evaluation_id"))
public class DeliveryPackageReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private SelfEvaluation evaluation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secretary_id", nullable = false)
    private Secretary secretary;
    @Column(name = "author_name", nullable = false, length = 150)
    private String authorName;
    @Column(name = "source_version", nullable = false, length = 64)
    private String sourceVersion;
    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;
    protected DeliveryPackageReview() {}
    public DeliveryPackageReview(SelfEvaluation evaluation) { this.evaluation = evaluation; }
    public void mark(Secretary secretary, String version, Instant now) {
        this.secretary = secretary; this.authorName = secretary.getName();
        this.sourceVersion = version; this.reviewedAt = now;
    }
    public SelfEvaluation getEvaluation() { return evaluation; }
    public String getAuthorName() { return authorName; }
    public String getSourceVersion() { return sourceVersion; }
    public Instant getReviewedAt() { return reviewedAt; }
}
