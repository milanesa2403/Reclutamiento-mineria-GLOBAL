-- V18: HU18/HU20 valoración pública de empresas y HU21 reportes de ofertas.
--
-- valoracion_empresa: solo un postulante CONFIRMADO valora a la empresa; una valoración por postulación (editable y
--   eliminable). id_empresa se guarda para calcular rápido el promedio de cada empresa. Es pública: no hay datos
--   del postulante en lo que se muestra.
-- reporte_oferta: un postulante reporta una oferta publicada (una vez por oferta). Solo lo ve el administrador,
--   dentro del detalle de la empresa, donde decide si advierte, suspende o bloquea.

CREATE TABLE valoracion_empresa (
    id_valoracion       BIGSERIAL    PRIMARY KEY,
    id_postulacion      BIGINT       NOT NULL UNIQUE REFERENCES postulacion(id_postulacion) ON DELETE CASCADE,
    id_empresa          BIGINT       NOT NULL REFERENCES empresa(id_empresa) ON DELETE CASCADE,
    estrellas           SMALLINT     NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
    comentario          VARCHAR(500),
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
CREATE INDEX ix_valoracion_empresa_empresa ON valoracion_empresa (id_empresa);
ALTER TABLE valoracion_empresa ENABLE ROW LEVEL SECURITY;

CREATE TABLE reporte_oferta (
    id_reporte      BIGSERIAL    PRIMARY KEY,
    id_convocatoria BIGINT       NOT NULL REFERENCES convocatoria(id_convocatoria) ON DELETE CASCADE,
    id_postulante   BIGINT       NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    motivo          VARCHAR(30)  NOT NULL CHECK (motivo IN ('DISCRIMINATORIA', 'SOLICITA_DINERO', 'ENGANOSA',
                                                            'NO_RESPONDEN', 'DUPLICADA', 'OTRO')),
    explicacion     VARCHAR(500) NOT NULL CHECK (btrim(explicacion) <> ''),
    fecha_reporte   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revisado        BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_revision  TIMESTAMP,
    CONSTRAINT uq_reporte_oferta UNIQUE (id_convocatoria, id_postulante)
);
CREATE INDEX ix_reporte_oferta_convocatoria ON reporte_oferta (id_convocatoria);
ALTER TABLE reporte_oferta ENABLE ROW LEVEL SECURITY;
