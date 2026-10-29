package com.sem.pmiautoevaluacion.pmi.repository;

import com.sem.pmiautoevaluacion.pmi.entity.Objective;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {
    List<Objective> findByPrioritaryComponent_Pmi_IdOrderByIdAsc(UUID pmiId);
}
