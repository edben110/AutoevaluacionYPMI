-- Con herencia JOINED, Hibernate espera una tabla por cada subclase concreta,
-- incluso si no tiene columnas propias. La tabla solo contiene la PK,
-- que a su vez es FK hacia "user".id_user.

CREATE TABLE secretary (
    id_user UUID PRIMARY KEY,
 
    CONSTRAINT fk_secretary_user
        FOREIGN KEY (id_user)
        REFERENCES "user" (id_user)
        ON DELETE CASCADE
);
