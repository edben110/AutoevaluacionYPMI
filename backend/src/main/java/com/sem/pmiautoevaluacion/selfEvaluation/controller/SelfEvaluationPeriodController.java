package com.sem.pmiautoevaluacion.selfEvaluation.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodRequest;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationPeriodResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationPeriodService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/self-evaluation-periods")
public class SelfEvaluationPeriodController {
    private final SelfEvaluationPeriodService service;

    public SelfEvaluationPeriodController(SelfEvaluationPeriodService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public List<SelfEvaluationPeriodResponse> list() { return service.list(); }

    @GetMapping("/{year}")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public SelfEvaluationPeriodResponse find(@PathVariable int year) { return service.find(year); }

    @PutMapping("/{year}")
    @PreAuthorize("hasRole('SECRETARY')")
    public SelfEvaluationPeriodResponse configure(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @Valid @RequestBody SelfEvaluationPeriodRequest request) {
        return service.configure(principal.getUser().getId(), year, request);
    }

    @DeleteMapping("/{year}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SECRETARY')")
    public void delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year) {
        service.delete(principal.getUser().getId(), year);
    }
}
