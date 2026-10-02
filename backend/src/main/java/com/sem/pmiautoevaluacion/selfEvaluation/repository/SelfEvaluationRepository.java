package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfEvaluationRepository extends JpaRepository<SelfEvaluation, UUID> {
    Optional<SelfEvaluation> findByEstablishment_IdAndYear(UUID establishmentId, int year);
    List<SelfEvaluation> findByEstablishment_IdOrderByYearDesc(UUID establishmentId);
}
