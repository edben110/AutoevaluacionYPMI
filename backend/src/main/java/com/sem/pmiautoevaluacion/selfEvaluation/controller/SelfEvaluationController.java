package com.sem.pmiautoevaluacion.selfEvaluation.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationSummary;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/self-evaluations")
public class SelfEvaluationController {
    private final SelfEvaluationService service;

    public SelfEvaluationController(SelfEvaluationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public List<SelfEvaluationSummary> list(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) UUID establishmentId) {
        return service.list(readEstablishmentId(principal, establishmentId));
    }

    @GetMapping("/{year}")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public SelfEvaluationResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId), year);
    }

    @PutMapping("/{year}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public SelfEvaluationResponse createOrFind(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year) {
        return service.createOrFind(principal.getUser().getId(), year);
    }

    @PutMapping("/{year}/valuations/{componentId}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public ValuationResponse saveValuation(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID componentId,
            @Valid @RequestBody ValuationRequest request) {
        return service.saveValuation(principal.getUser().getId(), year, componentId, request);
    }

    private UUID readEstablishmentId(CustomUserDetails principal, UUID requestedId) {
        if (principal.getUser() instanceof Secretary) {
            if (requestedId == null) {
                throw new BadRequestException("Secretaría debe indicar establishmentId de la institución a consultar");
            }
            return requestedId;
        }
        UUID ownId = principal.getUser().getId();
        if (requestedId != null && !requestedId.equals(ownId)) {
            throw new AccessDeniedException("Una institución solo puede consultar sus propias autoevaluaciones");
        }
        return ownId;
    }
}
