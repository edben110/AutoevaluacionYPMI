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

import com.sem.pmiautoevaluacion.dto.CreateProcessRequest;
import com.sem.pmiautoevaluacion.dto.ProcessResponse;
import com.sem.pmiautoevaluacion.service.ProcessService;
import com.sem.pmiautoevaluacion.entity.Process;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/processes")
public class ProcessController {
    private final ProcessService processService;

    public ProcessController(
        ProcessService processService
    ) {
        this.processService = processService;
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> create(
        @Valid @RequestBody CreateProcessRequest request
    ) {
        Process process = processService.create(
            request.areaId(),
            request.name(),
            request.description()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(process.getId())
            .toUri();
        return ResponseEntity.created(location)
            .body(ProcessResponse.from(process));
    }

    @GetMapping ("/{id}")
    public ProcessResponse findById(
        @PathVariable UUID id,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Process process = onlyActive
            ? processService.findActiveById(id)
            : processService.findById(id);
        return ProcessResponse.from(process);
    }

    @GetMapping ("/search")
    public ProcessResponse findByName(
        @RequestParam String name,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        Process process = onlyActive
            ? processService.findActiveByName(name)
            : processService.findByName(name);
        return ProcessResponse.from(process);
    }

    @GetMapping
    public List<ProcessResponse> findAll(
        @RequestParam (required = false) UUID areaId,
        @RequestParam (defaultValue = "false") boolean onlyActive
    ) {
        List<Process> processes;

        if (areaId != null) {
            processes = onlyActive
                ? processService.findActiveByAreaId(areaId)
                : processService.findByAreaId(areaId);
        } else {
            processes = onlyActive
                ? processService.findAllActive()
                : processService.findAll();
        }

        return processes.stream()
            .map(ProcessResponse::from)
            .toList();
    }
}
