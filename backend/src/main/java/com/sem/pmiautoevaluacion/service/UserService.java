package com.sem.pmiautoevaluacion.service;

import java.util.UUID;

import com.sem.pmiautoevaluacion.entity.User;

public interface UserService {
    User findById(UUID id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
