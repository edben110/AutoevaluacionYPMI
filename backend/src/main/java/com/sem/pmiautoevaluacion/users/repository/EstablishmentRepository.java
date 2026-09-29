package com.sem.pmiautoevaluacion.users.repository;

import com.sem.pmiautoevaluacion.entity.Establishment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID>{
    Optional<Establishment> findByDaneCode(String daneCode);

    boolean existsByDaneCode(String daneCode);
}
