package com.sem.pmiautoevaluacion.review.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.service.DeliveryService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
public class DeliveryController {
    private final DeliveryService service;
    public DeliveryController(DeliveryService service) { this.service = service; }

    @GetMapping("/deliveries")
    public List<DeliveryResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return service.list(principal.getUser() instanceof Secretary ? null : principal.getUser().getId());
    }

    @GetMapping("/{year}/areas/{areaId}")
    public DeliveryDetailResponse find(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId,
            @RequestParam(required = false) UUID establishmentId) {
        return service.find(readInstitution(principal, establishmentId), year, areaId);
    }

    static UUID readInstitution(CustomUserDetails principal, UUID requestedId) {
        if (principal.getUser() instanceof Secretary) {
            if (requestedId == null) throw new BadRequestException("Secretaría debe indicar establishmentId de la institución");
            return requestedId;
        }
        var ownId = principal.getUser().getId();
        if (requestedId != null && !ownId.equals(requestedId))
            throw new AccessDeniedException("Solo puedes consultar las entregas de tu institución");
        return ownId;
    }
}
