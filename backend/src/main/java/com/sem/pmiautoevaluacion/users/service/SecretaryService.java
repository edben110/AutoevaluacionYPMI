package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.Secretary;

public interface SecretaryService {
    Secretary create(
        String name,
        String password,
        String email
    );

    Secretary findById(UUID id);

    Secretary findByEmail(String email);
}
