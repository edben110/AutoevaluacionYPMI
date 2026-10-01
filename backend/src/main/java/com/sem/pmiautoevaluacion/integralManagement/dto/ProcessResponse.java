package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import com.sem.pmiautoevaluacion.integralManagement.entity.Process;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

// DTO de salida para procesos
public record ProcessResponse(
    UUID id,
    String name,
    String description,
    UseState state,
    UUID areaId
) {
    public static ProcessResponse from (Process process) {
        return new ProcessResponse(
            process.getId(),
            process.getName(),
            process.getDescription(),
            process.getState(),
            process.getArea().getId()
        );
    }
}
