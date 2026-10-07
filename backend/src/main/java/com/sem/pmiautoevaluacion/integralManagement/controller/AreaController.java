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

import com.sem.pmiautoevaluacion.integralManagement.dto.AreaResponse;
import com.sem.pmiautoevaluacion.integralManagement.dto.CreateAreaRequest;
import com.sem.pmiautoevaluacion.integralManagement.dto.UpdateAreaRequest;
import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.integralManagement.service.AreaService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/areas")
public class AreaController {
    private final AreaService areaService;

    public AreaController(
        AreaService areaService
    ) {
        this.areaService = areaService;
    }

    // Rutas Post - Creacion

    @PostMapping 
    public ResponseEntity<AreaResponse> create(
        @Valid @RequestBody CreateAreaRequest request
    ) {
        Area area = areaService.create(
            request.name(),
            request.description()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(area.getId())
            .toUri();
        return ResponseEntity.created(location)
            .body(AreaResponse.from(area));
    }

    // Rutas Put o Patch - Actualizacion

    @PutMapping ("/{id}")
    public AreaResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateAreaRequest request
    ) {
        return AreaResponse.from(
            areaService.update(
                id,
                request.name(),
                request.description()
            )
        );
    }

    @PatchMapping ("/{id}/deactivate")
    public AreaResponse deactivate(@PathVariable UUID id){
        return AreaResponse.from(areaService.deactivate(id));
    }

    @PatchMapping ("/{id}/activate")
    public AreaResponse activate(@PathVariable UUID id) {
        return AreaResponse.from(areaService.activate(id));
    }

    // Rutas Get - Extraccion

    @GetMapping ("/{id}")
    public AreaResponse findById(
        @PathVariable UUID id,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Area area = onlyActive
            ? areaService.findActiveById(id)
            : areaService.findById(id);
        return AreaResponse.from(area);
    }

    @GetMapping("/search")
    public AreaResponse findByName(
        @RequestParam String name,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Area area = onlyActive
            ? areaService.findActiveByName(name)
            : areaService.findByName(name);
        return AreaResponse.from(area);
    }

    @GetMapping 
    public List<AreaResponse> findAll(
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        List<Area> areas;

        areas = onlyActive
            ? areaService.findAllActive()
            : areaService.findAll();
        return areas.stream()
            .map(AreaResponse::from)
            .toList();
    }
}
