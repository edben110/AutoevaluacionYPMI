package com.sem.pmiautoevaluacion.integralManagement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.integralManagement.entity.Process;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

public interface ProcessRepository extends JpaRepository<Process,UUID>{
    Optional<Process> findByIdAndState(UUID id, UseState state);

    Optional<Process> findByName(String name);
    Optional<Process> findByNameAndState(String name, UseState state);

    List<Process> findByAreaId(UUID areaId);
    List<Process> findByAreaIdAndState(UUID areaId, UseState state);
    
    List<Process> findByState(UseState state);

    boolean existsByName(String name);
    boolean existsByIdAndState(UUID id, UseState state);
}
