package com.sem.pmiautoevaluacion.selfEvaluation.state;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;

/** Valores del diagrama de clases; level conserva la codificación existente en BD y API. */
public enum ComponentValue {
    EXISTENCE(1, "Existencia"),
    PERTINENCE(2, "Pertinencia"),
    APPROPRIATION(3, "Apropiación"),
    CONTINUOUS_IMPROVEMENT(4, "Mejoramiento continuo");

    private final short level;
    private final String label;

    ComponentValue(int level, String label) {
        this.level = (short) level;
        this.label = label;
    }

    public short getLevel() { return level; }
    public String getLabel() { return label; }

    public static ComponentValue fromLevel(int level) {
        for (ComponentValue value : values()) {
            if (value.level == level) return value;
        }
        throw new BadRequestException("Selecciona un estado de valoración válido (1 a 4)");
    }
}
