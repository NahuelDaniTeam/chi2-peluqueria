-- Etapa 2: horarios de atención y días en que no se atiende.

-- Franjas de atención semanales. Un día puede tener dos (ej. 9 a 13 y 16 a 20).
CREATE TABLE franja_horaria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id BIGINT  NOT NULL REFERENCES negocio (id),
    -- NULL = horario de atención del local; con valor = horario propio de ese barbero
    -- (si un barbero no tiene franjas propias, trabaja en el horario del local)
    usuario_id BIGINT  REFERENCES usuario (id),
    -- 1 = lunes … 7 = domingo
    dia_semana INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    -- Hora local de la barbería
    desde      TIME    NOT NULL,
    hasta      TIME    NOT NULL,
    CHECK (desde < hasta)
);

CREATE INDEX franja_horaria_negocio ON franja_horaria (negocio_id, usuario_id);

-- Días completos en que no se atiende: feriados y vacaciones del local, o ausencias de un barbero.
CREATE TABLE ausencia (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id BIGINT       NOT NULL REFERENCES negocio (id),
    -- NULL = cierra la barbería; con valor = no viene ese barbero
    usuario_id BIGINT       REFERENCES usuario (id),
    desde      DATE         NOT NULL,
    hasta      DATE         NOT NULL,
    motivo     VARCHAR(120),
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (desde <= hasta)
);

CREATE INDEX ausencia_negocio ON ausencia (negocio_id, hasta);
