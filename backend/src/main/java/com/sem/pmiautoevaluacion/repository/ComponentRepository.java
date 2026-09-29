package com.sem.pmiautoevaluacion.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.entity.Component;

public interface ComponentRepository extends JpaRepository<Component,UUID>{
    Optional<Component>findByName(String name);
    
    boolean existsByName(String name);

    List<Component> findByProcessId(UUID processId);
}
