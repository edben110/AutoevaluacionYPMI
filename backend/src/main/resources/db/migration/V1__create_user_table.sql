CREATE TABLE "user" (
    id_user UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,

    CONSTRAINT uq_user_email UNIQUE (email)
);