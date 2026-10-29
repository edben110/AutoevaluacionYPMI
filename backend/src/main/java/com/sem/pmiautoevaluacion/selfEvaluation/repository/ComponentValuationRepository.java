package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.ComponentValuation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentValuationRepository extends JpaRepository<ComponentValuation, UUID> {
    List<ComponentValuation> findByEvaluation_Id(UUID evaluationId);
    Optional<ComponentValuation> findByEvaluation_IdAndComponent_Id(UUID evaluationId, UUID componentId);
}
