package com.sem.pmiautoevaluacion.review.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.review.dto.*;
import com.sem.pmiautoevaluacion.review.service.ReviewCommentService;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/{year}/areas/{areaId}")
public class ReviewCommentController {
    private final ReviewCommentService service;
    public ReviewCommentController(ReviewCommentService service) { this.service = service; }

    @GetMapping("/comments")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public List<ReviewCommentResponse> list(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @RequestParam(required = false) UUID establishmentId) {
        return service.list(DeliveryController.readInstitution(principal, establishmentId), year, areaId);
    }

    @PostMapping("/components/{componentId}/comments")
    @PreAuthorize("hasRole('SECRETARY')")
    public ReviewCommentResponse add(@AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable int year, @PathVariable UUID areaId, @PathVariable UUID componentId,
            @RequestParam(required = false) UUID establishmentId, @Valid @RequestBody ReviewCommentRequest request) {
        if (establishmentId == null) throw new BadRequestException("Selecciona la institución que vas a revisar");
        return service.add(principal.getUser().getId(), establishmentId, year, areaId, componentId, request);
    }
}
