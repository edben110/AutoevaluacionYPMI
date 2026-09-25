package com.sem.pmiautoevaluacion.service;

import com.sem.pmiautoevaluacion.entity.Secretary;
import java.util.UUID;

public interface SecretaryService {
    Secretary create(
        String name,
        String password,
        String email
    );

    Secretary findById(UUID id);

    Secretary findByEmail(String email);
}
