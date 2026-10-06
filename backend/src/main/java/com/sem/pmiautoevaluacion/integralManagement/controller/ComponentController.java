package com.sem.pmiautoevaluacion.integralManagement.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sem.pmiautoevaluacion.integralManagement.dto.ChangeProcessRequest;
import com.sem.pmiautoevaluacion.integralManagement.dto.ComponentResponse;
import com.sem.pmiautoevaluacion.integralManagement.dto.CreateComponentRequest;
import com.sem.pmiautoevaluacion.integralManagement.dto.UpdateComponentRequest;
import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.service.ComponentService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/components")
public class ComponentController {
    private final ComponentService componentService;

    public ComponentController(
        ComponentService componentService
    ) {
        this.componentService = componentService;
    }

    // Rutas Post - Creacion

    @PostMapping 
    public ResponseEntity<ComponentResponse> create(
        @Valid @RequestBody CreateComponentRequest request
    ) {
        Component component = componentService.create(
            request.processId(),
            request.name(),
            request.description(),
            request.expectedEvidence()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(component.getId())
            .toUri();
        
        return ResponseEntity.created(location)
            .body(ComponentResponse.from(component));
    }

    // Rutas Put o Patch - Actualizacion

    @PutMapping ("/{id}")
    public ComponentResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateComponentRequest request
    ) {
        return ComponentResponse.from(
            componentService.update(
                id,
                request.name(),
                request.description(),
                request.expectedEvidence()
            )
        );
    }

    @PatchMapping ("/{id}/deactivate")
    public ComponentResponse deactivate(@PathVariable UUID id) {
        return ComponentResponse.from(componentService.deactivate(id));
    }

    @PatchMapping ("/{id}/activate")
    public ComponentResponse activate(@PathVariable UUID id) {
        return ComponentResponse.from(componentService.activate(id));
    }

    @PatchMapping ("/{id}/change_process")
    public ComponentResponse changeProcess(
        @PathVariable UUID id,
        @Valid @RequestBody ChangeProcessRequest request
    ) {
        return ComponentResponse.from(
            componentService.changeProcess(id, request.processId())
        );
    }

    // Rutas Get - Extraccion

    @GetMapping ("/{id}")
    public ComponentResponse findById(
        @PathVariable UUID id,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Component component = onlyActive
            ? componentService.findActiveById(id)
            : componentService.findById(id);
        return ComponentResponse.from(component);
    }

    @GetMapping ("/search")
    public ComponentResponse findByName(
        @RequestParam String name,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Component component = onlyActive
            ? componentService.findActiveByName(name)
            : componentService.findByName(name);
        return ComponentResponse.from(component);
    }

    @GetMapping 
    public List<ComponentResponse> findAll(
        @RequestParam (required = false) UUID processId,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        List<Component> components;

        if(processId != null) {
            components = onlyActive
                ? componentService.findActiveByProcessId(processId)
                : componentService.findByProcessId(processId);
        } else {
            components = onlyActive
                ? componentService.findAllActive()
                : componentService.findAll();
        }

        return components.stream()
            .map(ComponentResponse::from)
            .toList();
    }
}
