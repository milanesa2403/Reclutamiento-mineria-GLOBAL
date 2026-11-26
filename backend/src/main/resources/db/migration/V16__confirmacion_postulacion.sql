ALTER TABLE postulacion DROP CONSTRAINT IF EXISTS postulacion_estado_check;
ALTER TABLE postulacion ADD CONSTRAINT postulacion_estado_check
    CHECK (estado IN ('POSTULADO', 'EN_REVISION', 'ACEPTADO', 'DESCARTADO', 'CONFIRMADO', 'RETIRADO'));
ALTER TABLE postulacion
    ADD COLUMN fecha_aceptacion TIMESTAMP,
    ADD COLUMN motivo_retiro    VARCHAR(60),
    ADD COLUMN visto            BOOLEAN NOT NULL DEFAULT TRUE;
