package com.sem.pmiautoevaluacion.integralManagement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

public interface AreaRepository extends JpaRepository<Area,UUID>{
    Optional<Area> findByIdAndState(UUID id, UseState state);

    Optional<Area>findByName(String name);
    Optional<Area> findByNameAndState(String name, UseState state);
    
    List<Area> findByState(UseState state);

    boolean existsByName(String name);
    boolean existsByIdAndState(UUID id, UseState state);
}
