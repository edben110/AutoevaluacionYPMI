package com.sem.pmiautoevaluacion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sem.pmiautoevaluacion.dto.MeResponse;
import com.sem.pmiautoevaluacion.security.CustomUserDetails;

/**
 * Devuelve los datos del usuario del token JWT enviado
 * Esta fuera de /api/auth/* a proposito.
 * Este endpoint debe exigir autenticacion.
 * MeController
 */
@RestController 
@RequestMapping ("/api/me")
public class MeController {
    @GetMapping
    public ResponseEntity<MeResponse> me(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(MeResponse.fromEntity(userDetails.getUser()));
    }
}
