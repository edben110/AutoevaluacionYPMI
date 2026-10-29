package com.sem.pmiautoevaluacion.users.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Usuario de tipo Secretaría.
 * No agrega columnas propias: su única diferencia respecto a User
 * es el valor del discriminador "role" y las funcionalidades de negocio
 * que se implementarán en la capa de servicio (revisión de autoevaluaciones,
 * observaciones, etc.).
 *
 * Al no tener atributos propios, con InheritanceType.JOINED, Hibernate
 * NO requiere una tabla física "secretary": basta con la fila en "user"
 * marcada con role = 'SECRETARY'.
 */
@Entity
@DiscriminatorValue("SECRETARY")
public class Secretary extends User {

    protected Secretary() {
        super();
    }

    public Secretary(String name, String password, EmailRecord email) {
        super(name, password, email);
    }
}