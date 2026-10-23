package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationAreaSubmission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfEvaluationAreaSubmissionRepository extends JpaRepository<SelfEvaluationAreaSubmission, UUID> {
    Optional<SelfEvaluationAreaSubmission> findByEvaluation_IdAndArea_Id(UUID evaluationId, UUID areaId);
    boolean existsByEvaluation_IdAndArea_Id(UUID evaluationId, UUID areaId);
}
