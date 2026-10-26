package com.sem.pmiautoevaluacion.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.users.dto.UpdateMeRequest;
import com.sem.pmiautoevaluacion.users.dto.MeResponse;
import com.sem.pmiautoevaluacion.users.entity.User;
import com.sem.pmiautoevaluacion.users.service.UserService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;

import jakarta.validation.Valid;

/**
 * Devuelve los datos del usuario del token JWT enviado
 * Esta fuera de /api/auth/* a proposito.
 * Este endpoint debe exigir autenticacion.
 * MeController
 */
@RestController 
@RequestMapping ("/api/me")
public class MeController {
    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<MeResponse> me(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(MeResponse.fromEntity(userDetails.getUser()));
    }

    @PatchMapping
    public ResponseEntity<MeResponse> update(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateMeRequest request
    ) {
        if (request.name() == null
            && request.email() == null
            && request.password() == null
            && request.daneCode() == null
            && request.rector() == null) {
            throw new BadRequestException("Debes enviar al menos un campo para actualizar");
        }

        User updatedUser = userService.update(
            userDetails.getUser(),
            request.name(),
            request.email(),
            request.password(),
            request.daneCode(),
            request.rector()
        );

        return ResponseEntity.ok(MeResponse.fromEntity(updatedUser));
    }
}
