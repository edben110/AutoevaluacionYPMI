package com.sem.pmiautoevaluacion.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sem.pmiautoevaluacion.users.entity.Establishment;

import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID>{
    Optional<Establishment> findByDaneCode(String daneCode);

    boolean existsByDaneCode(String daneCode);
}
