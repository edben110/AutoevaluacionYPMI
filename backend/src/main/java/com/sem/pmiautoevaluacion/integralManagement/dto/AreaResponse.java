package com.sem.pmiautoevaluacion.integralManagement.dto;

import java.util.UUID;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

// DTO de salida para areas
public record AreaResponse(
    UUID id,
    String name,
    String description,
    UseState state
) {
    public static AreaResponse from (Area area) {
        return new AreaResponse(
            area.getId(),
            area.getName(),
            area.getDescription(),
            area.getState()
        );
    }
}
