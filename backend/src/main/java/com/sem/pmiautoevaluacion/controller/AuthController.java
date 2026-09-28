package com.sem.pmiautoevaluacion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sem.pmiautoevaluacion.dto.EstablishmentLoginRequest;
import com.sem.pmiautoevaluacion.dto.LoginResponse;
import com.sem.pmiautoevaluacion.dto.SecretaryLoginRequest;
import com.sem.pmiautoevaluacion.service.AuthService;

import jakarta.validation.Valid;

/**
 * Esta ruta: /api/auth
 * no tiene restricciones de acceso por autenticacion o token
 * esto para que puedan realizarse los logins correctamente
 * AuthController
 */
@RestController 
@RequestMapping ("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping ("/secretary/login")
    public ResponseEntity<LoginResponse> loginSecretary (
        @Valid @RequestBody SecretaryLoginRequest request
    ) {
        return ResponseEntity.ok(authService.loginSecretary(request));
    }

    @PostMapping ("/establishment/login")
    public ResponseEntity<LoginResponse> loginEstablishment (
        @Valid @RequestBody EstablishmentLoginRequest request
    ) {
        return ResponseEntity.ok(authService.loginEstablishment(request));
    }
}
