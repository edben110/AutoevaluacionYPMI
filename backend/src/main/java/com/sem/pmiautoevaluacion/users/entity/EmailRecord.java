package com.sem.pmiautoevaluacion.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Embeddable 
public class EmailRecord {
    private static final String EMAIL_PATTERN =
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @NotBlank(message = "El correo no puede estar vacio")
    @Pattern(regexp = EMAIL_PATTERN, message = "El correo no tiene un formato valido")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String value;

    protected EmailRecord(){
        // Constructor Vacio requerido por JPA
    }

    public EmailRecord(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override 
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailRecord)) return false;
        EmailRecord that = (EmailRecord) o;
        return value != null && value.equalsIgnoreCase(that.value);
    }

    @Override 
    public int hashCode() {
        return value == null ? 0 : value.toLowerCase().hashCode();
    }
}
