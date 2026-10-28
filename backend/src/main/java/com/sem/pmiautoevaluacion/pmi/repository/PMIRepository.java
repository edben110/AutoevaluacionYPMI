package com.sem.pmiautoevaluacion.pmi.repository;

import com.sem.pmiautoevaluacion.pmi.entity.PMI;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PMIRepository extends JpaRepository<PMI, UUID> {
    List<PMI> findBySelfEvaluation_IdIn(java.util.Collection<UUID> evaluationIds);
    Optional<PMI> findBySelfEvaluation_IdAndArea_Id(UUID selfEvaluationId, UUID areaId);
    List<PMI> findAllBySelfEvaluation_Id(UUID selfEvaluationId);
}
