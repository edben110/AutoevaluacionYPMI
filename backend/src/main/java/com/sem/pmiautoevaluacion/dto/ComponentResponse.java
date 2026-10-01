package com.sem.pmiautoevaluacion.dto;

import java.util.UUID;

import com.sem.pmiautoevaluacion.entity.Component;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

// DTO de salida para componentes
public record ComponentResponse(
    UUID id,
    String name,
    String description,
    UseState state,
    UUID processId
) {
    public static ComponentResponse from(Component component) {
        return new ComponentResponse(
            component.getId(),
            component.getName(),
            component.getDescription(),
            component.getState(),
            component.getProcess().getId()
        );
    }
}
