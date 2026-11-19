-- Etapa 1: barberías (negocios) y usuarios.
-- Todo dato que pertenece a una barbería lleva negocio_id (ver CLAUDE.md §4, "Aislamiento entre barberías").

CREATE TABLE negocio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    -- Identificador para el link público de la barbería (ej. turnos.chi2.com/barberia-juan)
    slug         VARCHAR(60)  NOT NULL UNIQUE,
    zona_horaria VARCHAR(60)  NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    -- false = suspendida por CHI2: nadie de la barbería puede entrar
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- NULL solo para los usuarios de CHI2 (rol ADMIN)
    negocio_id    BIGINT       REFERENCES negocio (id),
    nombre        VARCHAR(120) NOT NULL,
    -- Se guarda siempre en minúsculas
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'DUENO', 'BARBERO')),
    -- false = dado de baja por el dueño: no puede entrar
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT usuario_admin_sin_negocio CHECK ((rol = 'ADMIN') = (negocio_id IS NULL))
);

CREATE UNIQUE INDEX usuario_email_unico ON usuario (lower(email));
CREATE INDEX usuario_negocio ON usuario (negocio_id);
