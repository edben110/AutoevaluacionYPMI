package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.ObjectiveRequest;
import com.sem.pmiautoevaluacion.pmi.dto.PMIFormulationResponse;
import com.sem.pmiautoevaluacion.pmi.service.PMIFormulationService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pmi/{year}/areas/{areaId}/formulation")
public class PMIFormulationController {
    private final PMIFormulationService service;

    public PMIFormulationController(PMIFormulationService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public PMIFormulationResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId), year, areaId);
    }

    @PostMapping("/objectives")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public PMIFormulationResponse createObjective(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @Valid @RequestBody ObjectiveRequest request) {
        return service.createObjective(principal.getUser().getId(), year, areaId, request);
    }

    @PutMapping("/objectives/{objectiveId}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public PMIFormulationResponse updateObjective(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @PathVariable UUID objectiveId, @Valid @RequestBody ObjectiveRequest request) {
        return service.updateObjective(principal.getUser().getId(), year, areaId, objectiveId, request);
    }

    @DeleteMapping("/objectives/{objectiveId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public void deleteObjective(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @PathVariable UUID objectiveId) {
        service.deleteObjective(principal.getUser().getId(), year, areaId, objectiveId);
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
            throw new AccessDeniedException("Una institución solo puede consultar su propio PMI");
        }
        return ownId;
    }
}
