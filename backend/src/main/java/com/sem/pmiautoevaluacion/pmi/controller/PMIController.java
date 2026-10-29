package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.service.PMIService;
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
@RequestMapping("/api/pmi")
public class PMIController {
    private final PMIService service;
    private final PMIAccessService access;

    public PMIController(PMIService service, PMIAccessService access) { this.service = service; this.access = access; }

    @GetMapping("/{year}")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public PMIOverviewResponse overview(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year,
            @RequestParam(required = false) UUID establishmentId) {
        UUID institutionId = readEstablishmentId(principal, establishmentId, year, null);
        var result = service.overview(institutionId, year);
        return principal.getUser() instanceof Secretary ? access.submittedOverview(institutionId, result) : result;
    }

    @GetMapping("/{year}/areas/{areaId}")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public PMIResponse find(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year,
            @PathVariable UUID areaId, @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId, year, areaId), year, areaId);
    }

    @PutMapping("/{year}/areas/{areaId}/components/{componentId}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public PMIResponse saveComponent(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @PathVariable UUID componentId,
            @Valid @RequestBody PriorityComponentRequest request) {
        return service.saveComponent(principal.getUser().getId(), year, areaId, componentId, request);
    }

    @PutMapping("/{year}/areas/{areaId}/selection")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public PMIResponse select(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year,
            @PathVariable UUID areaId, @Valid @RequestBody PMISelectionRequest request) {
        return service.select(principal.getUser().getId(), year, areaId, request);
    }

    private UUID readEstablishmentId(CustomUserDetails principal, UUID requestedId, int year, UUID areaId) {
        if (principal.getUser() instanceof Secretary) {
            if (requestedId == null) {
                throw new BadRequestException("Secretaría debe indicar establishmentId de la institución a consultar");
            }
            if (areaId != null) access.requireSubmitted(requestedId, year, areaId);
            return requestedId;
        }
        UUID ownId = principal.getUser().getId();
        if (requestedId != null && !requestedId.equals(ownId)) {
            throw new AccessDeniedException("Una institución solo puede consultar su propio PMI");
        }
        return ownId;
    }
}
