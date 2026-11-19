-- Etapa 3: la agenda. Turnos y su historial de cambios.

-- Hace falta para que la restricción de abajo compare barbero_id (un número) dentro de un índice GiST.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE turno (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id         BIGINT       NOT NULL REFERENCES negocio (id),
    barbero_id         BIGINT       NOT NULL REFERENCES usuario (id),
    servicio_id        BIGINT       NOT NULL REFERENCES servicio (id),
    -- En UTC; se muestran en la zona horaria de la barbería. El largo es la duración del servicio.
    inicio             TIMESTAMPTZ  NOT NULL,
    fin                TIMESTAMPTZ  NOT NULL,
    -- El cliente no tiene cuenta: se lo anota con su nombre y, si lo deja, su teléfono
    cliente_nombre     VARCHAR(80)  NOT NULL,
    cliente_telefono   VARCHAR(30),
    nota               VARCHAR(200),
    -- RESERVADO → EN_CURSO → FINALIZADO, o CANCELADO / AUSENTE. Los de la atención llegan en la etapa 4.
    estado             VARCHAR(20)  NOT NULL
        CHECK (estado IN ('RESERVADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO', 'AUSENTE')),
    -- Cancelado con menos de 24 horas de aviso (cuenta aparte en las métricas)
    cancelacion_tardia BOOLEAN      NOT NULL DEFAULT FALSE,
    -- NULL cuando lo reservó el propio cliente desde el link (etapa 6)
    creado_por         BIGINT       REFERENCES usuario (id),
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (inicio < fin),
    CHECK (NOT cancelacion_tardia OR estado = 'CANCELADO'),
    -- Un barbero nunca tiene dos turnos superpuestos. Los cancelados y los ausentes liberan el lugar.
    -- (Igual que EstadoTurno.ocupaLugar().)
    CONSTRAINT turno_sin_superposicion EXCLUDE USING gist (
        barbero_id WITH =,
        tstzrange(inicio, fin) WITH &&
    ) WHERE (estado IN ('RESERVADO', 'EN_CURSO', 'FINALIZADO'))
);

CREATE INDEX turno_agenda ON turno (negocio_id, inicio);

-- Qué le pasó a cada turno, quién y cuándo (para la trazabilidad).
CREATE TABLE turno_cambio (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id BIGINT       NOT NULL REFERENCES negocio (id),
    turno_id   BIGINT       NOT NULL REFERENCES turno (id),
    -- NULL = lo hizo el cliente desde el link (etapa 6)
    usuario_id BIGINT       REFERENCES usuario (id),
    -- CREADO, REPROGRAMADO, CANCELADO… (ver AccionTurno; sin CHECK para sumar acciones sin migrar)
    accion     VARCHAR(30)  NOT NULL,
    detalle    VARCHAR(200),
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX turno_cambio_turno ON turno_cambio (turno_id);
