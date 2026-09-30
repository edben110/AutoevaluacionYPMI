package com.sem.pmiautoevaluacion.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.entity.Component;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

public interface ComponentRepository extends JpaRepository<Component,UUID>{
    Optional<Component> findByIdAndState(UUID id, UseState state);

    Optional<Component>findByName(String name);
    Optional<Component> findByNameAndState(String name, UseState state);
    
    List<Component> findByProcessId(UUID processId);
    List<Component> findByProcessIdAndState(UUID processId, UseState state);

    List<Component> findByState(UseState state);

    boolean existsByName(String name);
}
