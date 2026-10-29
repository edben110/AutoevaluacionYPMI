package com.sem.pmiautoevaluacion.review.repository;

import com.sem.pmiautoevaluacion.review.entity.ComponentReviewComment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentReviewCommentRepository extends JpaRepository<ComponentReviewComment, UUID> {
    List<ComponentReviewComment> findBySubmission_Evaluation_Id(UUID evaluationId);
    List<ComponentReviewComment> findBySubmission_IdOrderByCreatedAtAscIdAsc(UUID submissionId);
}
