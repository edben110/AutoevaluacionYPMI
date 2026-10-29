package com.sem.pmiautoevaluacion.review.dto;

import com.sem.pmiautoevaluacion.review.entity.ComponentReviewComment;
import java.time.Instant;
import java.util.UUID;

public record ReviewCommentResponse(UUID id, UUID componentId, UUID authorId, String authorName,
        String body, Instant createdAt) {
    public static ReviewCommentResponse from(ComponentReviewComment comment) {
        return new ReviewCommentResponse(comment.getId(), comment.getValuation().getComponent().getId(),
                comment.getSecretary().getId(), comment.getAuthorName(), comment.getBody(), comment.getCreatedAt());
    }
}
