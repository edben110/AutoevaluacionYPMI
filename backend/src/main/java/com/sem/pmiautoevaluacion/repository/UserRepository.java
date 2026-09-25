package com.sem.pmiautoevaluacion.repository;

import com.sem.pmiautoevaluacion.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>{
    /**
     * Busca un usuario (de cualquier subtipo) por su email.
     * Como email es un @Embeddable (EmailRecord), no podemos usar
     * el "derived query" simple findByEmail(String); en su lugar
     * navegamos hasta el campo interno "value" del embebido.
     *
     * Este método será usado principalmente para el login de Secretary,
     * ya que Establishment inicia sesión por daneCode, no por email.
     */
    Optional<User> findByEmail_Value(String email);

    /**
     * Verifica existencia por email, útil para validar unicidad
     * antes de registrar un nuevo usuario (secretaría o establecimiento).
     */
    boolean existsByEmail_Value(String email);
}
