package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.Establishment;

public interface EstablishmentService {
    Establishment create(
        String name,
        String password,
        String email,
        String daneCode,
        String rector
    );

    Establishment findById(UUID id);
    Establishment findByDaneCode(String daneCode);

}
