package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationAreaSubmission;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfEvaluationAreaSubmissionRepository extends JpaRepository<SelfEvaluationAreaSubmission, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"evaluation", "evaluation.establishment", "area"})
    @org.springframework.data.jpa.repository.Query("select s from SelfEvaluationAreaSubmission s")
    List<SelfEvaluationAreaSubmission> findAllForReview();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"evaluation", "evaluation.establishment", "area"})
    List<SelfEvaluationAreaSubmission> findByEvaluation_Establishment_Id(UUID establishmentId);
    List<SelfEvaluationAreaSubmission> findByEvaluation_Id(UUID evaluationId);
    Optional<SelfEvaluationAreaSubmission> findByEvaluation_IdAndArea_Id(UUID evaluationId, UUID areaId);
    boolean existsByEvaluation_IdAndArea_Id(UUID evaluationId, UUID areaId);
}
