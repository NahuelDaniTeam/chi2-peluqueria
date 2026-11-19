-- Etapa 2: perfil de cada barbero — si atiende clientes y qué servicios hace.

-- Quién aparece en la agenda. Los barberos sí; el dueño solo si también corta.
ALTER TABLE usuario ADD COLUMN atiende BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE usuario SET atiende = TRUE WHERE rol = 'BARBERO';
ALTER TABLE usuario ADD CONSTRAINT usuario_admin_no_atiende CHECK (rol <> 'ADMIN' OR NOT atiende);

-- Servicios que hace cada barbero (si no está la fila, no lo hace)
CREATE TABLE barbero_servicio (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id      BIGINT  NOT NULL REFERENCES negocio (id),
    usuario_id      BIGINT  NOT NULL REFERENCES usuario (id),
    servicio_id     BIGINT  NOT NULL REFERENCES servicio (id),
    -- NULL = cobra el precio del local; con valor = precio distinto para este barbero (en pesos)
    precio_especial INTEGER CHECK (precio_especial >= 0),
    UNIQUE (usuario_id, servicio_id)
);

CREATE INDEX barbero_servicio_negocio ON barbero_servicio (negocio_id, usuario_id);
