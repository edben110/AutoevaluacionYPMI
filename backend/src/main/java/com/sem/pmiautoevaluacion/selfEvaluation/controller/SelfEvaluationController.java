package com.sem.pmiautoevaluacion.selfEvaluation.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationSummary;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.ValuationResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/self-evaluations")
@PreAuthorize("hasRole('ESTABLISHMENT')")
public class SelfEvaluationController {
    private final SelfEvaluationService service;

    public SelfEvaluationController(SelfEvaluationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SelfEvaluationSummary> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return service.list(principal.getUser().getId());
    }

    @GetMapping("/{year}")
    public SelfEvaluationResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year) {
        return service.find(principal.getUser().getId(), year);
    }

    @PutMapping("/{year}")
    public SelfEvaluationResponse createOrFind(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year) {
        return service.createOrFind(principal.getUser().getId(), year);
    }

    @PutMapping("/{year}/valuations/{componentId}")
    public ValuationResponse saveValuation(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID componentId,
            @Valid @RequestBody ValuationRequest request) {
        return service.saveValuation(principal.getUser().getId(), year, componentId, request);
    }
}
