package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.users.entity.User;

public interface UserService {
    User findById(UUID id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
