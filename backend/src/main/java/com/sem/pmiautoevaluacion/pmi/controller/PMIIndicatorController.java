package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.service.PMIIndicatorService;
import com.sem.pmiautoevaluacion.pmi.service.PMIAccessService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pmi/{year}/areas/{areaId}/indicators")
public class PMIIndicatorController {
    private final PMIIndicatorService service;
    private final PMIAccessService access;
    public PMIIndicatorController(PMIIndicatorService service, PMIAccessService access) { this.service = service; this.access = access; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public PMIIndicatorsResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId, year, areaId), year, areaId);
    }

    @PutMapping("/{indicatorId}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public IndicatorSheetResponse save(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @PathVariable UUID indicatorId,
            @Valid @RequestBody IndicatorDefinitionRequest request) {
        return service.save(principal.getUser().getId(), year, areaId, indicatorId, request);
    }

    private UUID readEstablishmentId(CustomUserDetails principal, UUID requestedId, int year, UUID areaId) {
        if (principal.getUser() instanceof Secretary) {
            if (requestedId == null) throw new BadRequestException("Secretaría debe indicar establishmentId de la institución a consultar");
            access.requireSubmitted(requestedId, year, areaId);
            return requestedId;
        }
        UUID ownId = principal.getUser().getId();
        if (requestedId != null && !requestedId.equals(ownId)) {
            throw new AccessDeniedException("Una institución solo puede consultar sus propios indicadores");
        }
        return ownId;
    }
}
