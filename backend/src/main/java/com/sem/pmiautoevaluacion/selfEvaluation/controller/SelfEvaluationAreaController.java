package com.sem.pmiautoevaluacion.selfEvaluation.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.selfEvaluation.dto.SelfEvaluationAreaResponse;
import com.sem.pmiautoevaluacion.selfEvaluation.service.SelfEvaluationAreaService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/self-evaluations/{year}/areas/{areaId}")
public class SelfEvaluationAreaController {
    private final SelfEvaluationAreaService service;

    public SelfEvaluationAreaController(SelfEvaluationAreaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public SelfEvaluationAreaResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId,
            @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId), year, areaId);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public SelfEvaluationAreaResponse submit(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId) {
        return service.submit(principal.getUser().getId(), year, areaId);
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
