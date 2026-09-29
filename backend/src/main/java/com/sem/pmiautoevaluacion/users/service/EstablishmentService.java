package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.Establishment;

public interface EstablishmentService {
    Establishment create(
        String rector,
        String daneCode,
        String password,

        //TODO: Si se remueven los emails para establishment:
        String name,
        String email
    );

    Establishment findById(UUID id);
    Establishment findByDaneCode(String daneCode);

}
