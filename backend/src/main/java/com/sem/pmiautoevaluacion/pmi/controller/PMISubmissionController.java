package com.sem.pmiautoevaluacion.pmi.controller;

import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.pmi.dto.PMISubmissionResponse;
import com.sem.pmiautoevaluacion.pmi.service.PMISubmissionService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pmi/{year}/areas/{areaId}/submission")
@PreAuthorize("hasRole('ESTABLISHMENT')")
public class PMISubmissionController {
    private final PMISubmissionService service;
    public PMISubmissionController(PMISubmissionService service) { this.service = service; }
    @GetMapping
    public PMISubmissionResponse find(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year, @PathVariable UUID areaId) {
        return service.find(principal.getUser().getId(), year, areaId);
    }
    @PostMapping
    public PMISubmissionResponse submit(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable int year, @PathVariable UUID areaId) {
        return service.submit(principal.getUser().getId(), year, areaId);
    }
}
