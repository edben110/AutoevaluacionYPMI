package com.sem.pmiautoevaluacion.pmi.repository;

import com.sem.pmiautoevaluacion.pmi.entity.PrioritaryComponent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrioritaryComponentRepository extends JpaRepository<PrioritaryComponent, UUID> {
    List<PrioritaryComponent> findByPmi_Id(UUID pmiId);
}
