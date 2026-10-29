package com.sem.pmiautoevaluacion.integralManagement.controller;

import com.sem.pmiautoevaluacion.integralManagement.dto.ComponentGuidanceResponse;
import com.sem.pmiautoevaluacion.integralManagement.service.ComponentGuidanceService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/components")
public class ComponentGuidanceController {
    private final ComponentGuidanceService service;

    public ComponentGuidanceController(ComponentGuidanceService service) { this.service = service; }

    @GetMapping("/{componentId}/guidance")
    @PreAuthorize("hasAnyRole('ESTABLISHMENT', 'SECRETARY')")
    public ComponentGuidanceResponse find(@PathVariable UUID componentId) {
        return service.find(componentId);
    }
}
