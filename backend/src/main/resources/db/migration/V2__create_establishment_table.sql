CREATE TABLE establishment (
    id_establishment UUID PRIMARY KEY,
    dane_code        VARCHAR(20)  NOT NULL,
    rector           VARCHAR(150) NOT NULL,
 
    CONSTRAINT uq_establishment_dane_code UNIQUE (dane_code),
 
    CONSTRAINT fk_establishment_user
        FOREIGN KEY (id_establishment)
        REFERENCES "user" (id_user)
        ON DELETE CASCADE
);
