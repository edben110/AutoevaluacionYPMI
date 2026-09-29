package com.sem.pmiautoevaluacion.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.entity.Process;

public interface ProcessRepository extends JpaRepository<Process,UUID>{
    Optional<Process> findByName(String name);

    boolean existsByName(String name);

    List<Process> findByAreaId(UUID areaId);
}
