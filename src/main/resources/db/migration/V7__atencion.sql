-- Etapa 4: la atención "tipo Uber". Empezar, finalizar con el cobro marcado, ausente y atención sin turno.

ALTER TABLE turno
    -- Alguien que entró sin reservar: se carga en el momento (o ya atendido) para que cuente en las métricas
    ADD COLUMN sin_turno     BOOLEAN     NOT NULL DEFAULT FALSE,
    -- Cuándo se tocó "Empezar" y "Finalizar" (en UTC). Empezado puede faltar: se puede finalizar sin empezar
    ADD COLUMN empezado_en   TIMESTAMPTZ,
    ADD COLUMN finalizado_en TIMESTAMPTZ,
    -- Lo que se cobró, en pesos sin centavos, y cómo. Solo se marca: el sistema no maneja dinero
    ADD COLUMN monto         INTEGER     CHECK (monto >= 0),
    ADD COLUMN medio_pago    VARCHAR(20) CHECK (medio_pago IN ('EFECTIVO', 'TRANSFERENCIA')),
    -- Un turno finalizado siempre tiene su cobro, y solo los finalizados lo tienen
    ADD CONSTRAINT turno_cobro CHECK (
        (estado = 'FINALIZADO') = (monto IS NOT NULL AND medio_pago IS NOT NULL AND finalizado_en IS NOT NULL));

-- A quien llega sin turno no siempre se le pregunta el nombre
ALTER TABLE turno ALTER COLUMN cliente_nombre DROP NOT NULL;
ALTER TABLE turno ADD CONSTRAINT turno_cliente CHECK (sin_turno OR cliente_nombre IS NOT NULL);

-- La atención sin turno queda afuera de la regla de superposición: ya pasó (o está pasando), y el barbero a
-- veces la mete entre dos turnos aunque se pise unos minutos. Igual ocupa lugar: la agenda no ofrece esos
-- horarios a otros (ver Disponibilidad).
ALTER TABLE turno DROP CONSTRAINT turno_sin_superposicion;
ALTER TABLE turno ADD CONSTRAINT turno_sin_superposicion EXCLUDE USING gist (
    barbero_id WITH =,
    tstzrange(inicio, fin) WITH &&
) WHERE (estado IN ('RESERVADO', 'EN_CURSO', 'FINALIZADO') AND NOT sin_turno);
