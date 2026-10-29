package com.sem.pmiautoevaluacion.review.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.service.DeliveryPackageService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/packages")
public class DeliveryPackageController {
    private final DeliveryPackageService service;
    public DeliveryPackageController(DeliveryPackageService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public List<DeliveryPackageResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return service.list(principal.getUser() instanceof Secretary ? null : principal.getUser().getId());
    }
    @PutMapping("/{year}/review")
    @PreAuthorize("hasRole('SECRETARY')")
    public DeliveryPackageResponse mark(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year,
            @RequestParam(required = false) UUID establishmentId, @Valid @RequestBody PackageReviewRequest request) {
        return service.mark(principal.getUser().getId(), institution(establishmentId), year, request.version());
    }
    @DeleteMapping("/{year}/review")
    @PreAuthorize("hasRole('SECRETARY')")
    public DeliveryPackageResponse pending(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year,
            @RequestParam(required = false) UUID establishmentId) {
        return service.pending(principal.getUser().getId(), institution(establishmentId), year);
    }
    private UUID institution(UUID id) {
        if (id == null) throw new BadRequestException("Selecciona la institución del paquete");
        return id;
    }
}
