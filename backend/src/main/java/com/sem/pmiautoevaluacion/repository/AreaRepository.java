package com.sem.pmiautoevaluacion.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.entity.Area;

public interface AreaRepository extends JpaRepository<Area,UUID>{
    Optional<Area>findByName(String name);

    boolean existsByName(String name);
}
