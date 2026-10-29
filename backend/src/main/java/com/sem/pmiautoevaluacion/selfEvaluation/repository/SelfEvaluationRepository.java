package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SelfEvaluationRepository extends JpaRepository<SelfEvaluation, UUID> {
    Optional<SelfEvaluation> findByEstablishment_IdAndYear(UUID establishmentId, int year);
    List<SelfEvaluation> findByEstablishment_IdOrderByYearDesc(UUID establishmentId);
    List<SelfEvaluation> findByYear(int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select evaluation from SelfEvaluation evaluation where evaluation.year = :year order by evaluation.id")
    List<SelfEvaluation> findByYearForPmiDeletion(@Param("year") int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select evaluation from SelfEvaluation evaluation where evaluation.establishment.id = :establishmentId and evaluation.year = :year")
    Optional<SelfEvaluation> findForPmiUpdate(@Param("establishmentId") UUID establishmentId,
            @Param("year") int year);
}
