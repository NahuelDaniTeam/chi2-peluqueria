-- Etapa 2: catálogo de servicios de cada barbería (lo carga el dueño).
-- La duración y el precio son del local; un barbero puede tener un precio especial (V4).

CREATE TABLE servicio (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id       BIGINT       NOT NULL REFERENCES negocio (id),
    nombre           VARCHAR(80)  NOT NULL,
    -- Cuánto ocupa en la agenda, en minutos (múltiplo de 5)
    duracion_minutos INTEGER      NOT NULL CHECK (duracion_minutos BETWEEN 5 AND 480 AND duracion_minutos % 5 = 0),
    -- En pesos, sin centavos
    precio           INTEGER      NOT NULL CHECK (precio >= 0),
    -- false = quitado del catálogo (se conserva por los turnos que ya lo usaron)
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX servicio_negocio ON servicio (negocio_id);
-- No puede haber dos servicios activos con el mismo nombre en una barbería
CREATE UNIQUE INDEX servicio_nombre_unico ON servicio (negocio_id, lower(nombre)) WHERE activo;
