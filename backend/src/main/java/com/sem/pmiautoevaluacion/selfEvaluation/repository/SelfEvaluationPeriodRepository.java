package com.sem.pmiautoevaluacion.selfEvaluation.repository;

import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluationPeriod;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SelfEvaluationPeriodRepository extends JpaRepository<SelfEvaluationPeriod, Integer> {
    List<SelfEvaluationPeriod> findAllByOrderByYearDesc();

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from SelfEvaluationPeriod p where p.year = :year")
    Optional<SelfEvaluationPeriod> findForDiligence(@Param("year") int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SelfEvaluationPeriod p where p.year = :year")
    Optional<SelfEvaluationPeriod> findForConfiguration(@Param("year") int year);
}
