-- V9: Postulaciones (HU014 postular · HU015 revisar y filtrar). Completa la base de convocatorias (V8).
--
-- Modelo:
--   empresa 1 ── * convocatoria            (V8: una empresa publica muchas convocatorias)
--   convocatoria 1 ── * postulacion * ── 1 postulante
--                                          (un postulante postula a muchas convocatorias; una convocatoria
--                                           recibe muchos postulantes; la relación es "postulacion")
--   postulacion * ── * certificacion       (certificados relevantes que el postulante adjunta al postular)
--
-- Decisiones:
--  * UNIQUE (convocatoria, postulante): la BD garantiza que nadie postula dos veces a la misma convocatoria.
--  * CV, foto del DNI, CUL y EMO NO se copian: la empresa ve los documentos VIGENTES del postulante (postulante.url_*).
--    Los certificados sí se eligen por postulación (HU014: "certificados relevantes").
--  * Que solo se postule a convocatorias PUBLICADAS y vigentes (fecha_cierre) lo valida el backend: depende de otra
--    tabla y del día actual, algo que un CHECK no puede expresar.
--  * id_usuario_revisor: SOLO trazabilidad de quién cambió el estado (HU015); no define permisos.

CREATE TABLE postulacion (
    id_postulacion       BIGSERIAL   PRIMARY KEY,
    id_convocatoria      BIGINT      NOT NULL REFERENCES convocatoria(id_convocatoria) ON DELETE CASCADE,
    id_postulante        BIGINT      NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    estado               VARCHAR(20) NOT NULL DEFAULT 'POSTULADO'
                         CHECK (estado IN ('POSTULADO', 'EN_REVISION', 'ACEPTADO', 'DESCARTADO')),
    fecha_postulacion    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cambio_estado  TIMESTAMP,                                   -- último cambio hecho por la empresa
    id_usuario_revisor   BIGINT      REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    CONSTRAINT uq_postulacion UNIQUE (id_convocatoria, id_postulante),
    CONSTRAINT ck_postulacion_revisada CHECK (estado = 'POSTULADO' OR fecha_cambio_estado IS NOT NULL)
);
-- HU015: postulantes de una convocatoria, filtrados por estado. (uq_postulacion ya indexa por id_convocatoria.)
CREATE INDEX ix_postulacion_convocatoria_estado ON postulacion (id_convocatoria, estado);
-- HU014: "mis postulaciones", las más recientes primero.
CREATE INDEX ix_postulacion_postulante ON postulacion (id_postulante, fecha_postulacion DESC);
ALTER TABLE postulacion ENABLE ROW LEVEL SECURITY;

-- Certificados adjuntos a cada postulación. Si el postulante elimina la certificación de su perfil, deja de figurar.
CREATE TABLE postulacion_certificacion (
    id_postulacion   BIGINT NOT NULL REFERENCES postulacion(id_postulacion) ON DELETE CASCADE,
    id_certificacion BIGINT NOT NULL REFERENCES certificacion(id_certificacion) ON DELETE CASCADE,
    PRIMARY KEY (id_postulacion, id_certificacion)
);
CREATE INDEX ix_postulacion_certificacion_cert ON postulacion_certificacion (id_certificacion);
ALTER TABLE postulacion_certificacion ENABLE ROW LEVEL SECURITY;

-- HU015: filtrar candidatos por certificaciones (búsqueda por nombre sin distinguir mayúsculas).
CREATE INDEX IF NOT EXISTS ix_certificacion_nombre ON certificacion (lower(nombre));
