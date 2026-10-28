package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.SelfEvaluationPrerequisiteResponse;
import com.sem.pmiautoevaluacion.pmi.service.PMIFollowUpCycleService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/self-evaluations/{year}/prerequisites")
public class SelfEvaluationPrerequisiteController {
    private final PMIFollowUpCycleService service;
    public SelfEvaluationPrerequisiteController(PMIFollowUpCycleService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public SelfEvaluationPrerequisiteResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @RequestParam(required = false) UUID establishmentId) {
        UUID own = principal.getUser().getId();
        if (principal.getUser() instanceof Secretary) {
            if (establishmentId == null) throw new BadRequestException("Secretaría debe indicar establishmentId");
            return service.prerequisites(establishmentId, year);
        }
        if (establishmentId != null && !establishmentId.equals(own)) throw new AccessDeniedException("Solo puedes consultar tus propios requisitos");
        return service.prerequisites(own, year);
    }
}
