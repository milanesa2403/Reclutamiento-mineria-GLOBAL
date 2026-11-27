ALTER TABLE postulacion DROP CONSTRAINT IF EXISTS postulacion_estado_check;
ALTER TABLE postulacion ADD CONSTRAINT postulacion_estado_check
    CHECK (estado IN ('POSTULADO', 'EN_REVISION', 'ENTREVISTA', 'ACEPTADO', 'DESCARTADO', 'CONFIRMADO', 'RETIRADO'));
ALTER TABLE postulacion
    ADD COLUMN entrevista_fecha     TIMESTAMP,
    ADD COLUMN entrevista_modalidad VARCHAR(10),
    ADD COLUMN entrevista_lugar     VARCHAR(255),
    ADD COLUMN entrevista_mensaje   VARCHAR(500);
