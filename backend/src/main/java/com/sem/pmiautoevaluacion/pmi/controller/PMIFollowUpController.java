package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.*;
import com.sem.pmiautoevaluacion.pmi.service.PMIFollowUpService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pmi/{year}/areas/{areaId}/follow-ups")
public class PMIFollowUpController {
    private final PMIFollowUpService service;
    public PMIFollowUpController(PMIFollowUpService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public PMIFollowUpResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate controlDate,
            @RequestParam(required = false) UUID establishmentId) {
        return service.find(readEstablishmentId(principal, establishmentId), year, areaId, controlDate);
    }

    @PutMapping("/{controlDate}/rows/{rowId}")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public FollowUpRowResponse save(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate controlDate,
            @PathVariable UUID rowId, @Valid @RequestBody FollowUpRequest request) {
        return service.save(principal.getUser().getId(), year, areaId, controlDate, rowId, request);
    }

    private UUID readEstablishmentId(CustomUserDetails principal, UUID requestedId) {
        if (principal.getUser() instanceof Secretary) {
            if (requestedId == null) throw new BadRequestException("Secretaría debe indicar establishmentId de la institución a consultar");
            return requestedId;
        }
        UUID ownId = principal.getUser().getId();
        if (requestedId != null && !requestedId.equals(ownId)) throw new AccessDeniedException("Una institución solo puede consultar su propio seguimiento");
        return ownId;
    }
}
