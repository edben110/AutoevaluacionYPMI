package com.sem.pmiautoevaluacion.review.entity;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.*;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "component_review_comment")
public class ComponentReviewComment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SelfEvaluationAreaSubmission submission;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valuation_id", nullable = false)
    private ComponentValuation valuation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secretary_id", nullable = false)
    private Secretary secretary;
    @Column(name = "author_name", nullable = false, length = 150)
    private String authorName;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ComponentReviewComment() {}
    public ComponentReviewComment(SelfEvaluationAreaSubmission submission, ComponentValuation valuation,
            Secretary secretary, String body, Instant now) {
        this.submission = submission; this.valuation = valuation; this.secretary = secretary;
        this.authorName = secretary.getName(); this.body = body; this.createdAt = now;
    }
    public UUID getId() { return id; }
    public ComponentValuation getValuation() { return valuation; }
    public Secretary getSecretary() { return secretary; }
    public String getAuthorName() { return authorName; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
