package com.sem.pmiautoevaluacion.users.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sem.pmiautoevaluacion.users.dto.CreateEstablishmentRequest;
import com.sem.pmiautoevaluacion.users.dto.CreateSecretaryRequest;
import com.sem.pmiautoevaluacion.users.dto.EstablishmentResponse;
import com.sem.pmiautoevaluacion.users.dto.UserResponse;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.service.EstablishmentService;
import com.sem.pmiautoevaluacion.users.service.SecretaryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('SECRETARY')")
public class UserRegistrationController {
    private final EstablishmentService establishmentService;
    private final SecretaryService secretaryService;

    public UserRegistrationController(
        EstablishmentService establishmentService,
        SecretaryService secretaryService
    ) {
        this.establishmentService = establishmentService;
        this.secretaryService = secretaryService;
    }

    @PostMapping("/establishments")
    public ResponseEntity<EstablishmentResponse> createEstablishment(
        @Valid @RequestBody CreateEstablishmentRequest request
    ) {
        Establishment establishment = establishmentService.create(
            request.name(),
            request.password(),
            request.email(),
            request.daneCode(),
            request.rector()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(establishment.getId())
            .toUri();

        return ResponseEntity.created(location)
            .body(EstablishmentResponse.fromEntity(establishment));
    }

    @PostMapping("/secretaries")
    public ResponseEntity<UserResponse> createSecretary(
        @Valid @RequestBody CreateSecretaryRequest request
    ) {
        Secretary secretary = secretaryService.create(
            request.name(),
            request.password(),
            request.email()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(secretary.getId())
            .toUri();

        return ResponseEntity.created(location)
            .body(UserResponse.fromEntity(secretary));
    }
}
