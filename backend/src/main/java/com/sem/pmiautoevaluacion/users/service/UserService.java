package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.User;

public interface UserService {
    User findById(UUID id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User update(
        User user,
        String name,
        String email,
        String password,
        String daneCode,
        String rector
    );

    void deleteById(UUID id);
}
