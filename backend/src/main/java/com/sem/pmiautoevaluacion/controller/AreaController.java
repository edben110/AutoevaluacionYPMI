package com.sem.pmiautoevaluacion.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sem.pmiautoevaluacion.dto.AreaResponse;
import com.sem.pmiautoevaluacion.dto.CreateAreaRequest;
import com.sem.pmiautoevaluacion.entity.Area;
import com.sem.pmiautoevaluacion.service.AreaService;

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
