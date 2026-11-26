-- V11: Auditoría de estados de empresas (dashboard del administrador: "Actividad reciente").
--
-- Una fila por cada cambio importante de una solicitud de empresa:
--   REGISTRADA        la empresa creó su cuenta
--   DOCUMENTO_ENVIADO la empresa subió su documento (queda EN_REVISION)
--   APROBADA | OBSERVADA | RECHAZADA   decisión del administrador (con motivo y quién la tomó)
--
-- Decisiones:
--  * id_empresa usa ON DELETE SET NULL: cuando la purga elimina una empresa RECHAZADA, el rastro se conserva
--    (RUC, razón social y motivo; sin correos, DNI ni nombres de personas), igual que empresa_rechazo_historial.
--  * ruc y razon_social se copian en cada fila para que el rastro no dependa de que la empresa siga existiendo.
--  * realizado_por es el correo del administrador (NULL cuando el actor es la propia empresa).
--  * fecha en UTC (el backend trabaja en UTC; ver ReclutamientoApplication).

CREATE TABLE IF NOT EXISTS actividad_empresa (
                                                 id_actividad  BIGSERIAL PRIMARY KEY,
                                                 id_empresa    BIGINT       REFERENCES empresa(id_empresa) ON DELETE SET NULL,
    ruc           VARCHAR(11)  NOT NULL,
    razon_social  VARCHAR(200) NOT NULL,
    tipo          VARCHAR(30)  NOT NULL
    CHECK (tipo IN ('REGISTRADA', 'DOCUMENTO_ENVIADO', 'APROBADA', 'OBSERVADA', 'RECHAZADA')),
    motivo        VARCHAR(500),
    realizado_por VARCHAR(150),
    fecha         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
CREATE INDEX IF NOT EXISTS ix_actividad_empresa_fecha   ON actividad_empresa (fecha DESC);
CREATE INDEX IF NOT EXISTS ix_actividad_empresa_empresa ON actividad_empresa (id_empresa, fecha DESC);
ALTER TABLE actividad_empresa ENABLE ROW LEVEL SECURITY;

-- Datos históricos: solo lo que la BD ya sabe con certeza (fecha de registro y ÚLTIMA decisión de cada empresa).
-- Las decisiones intermedias anteriores a esta migración no se guardaron nunca, así que no se inventan.
INSERT INTO actividad_empresa (id_empresa, ruc, razon_social, tipo, fecha)
SELECT e.id_empresa, e.ruc, e.razon_social, 'REGISTRADA', e.fecha_registro
FROM empresa e;

INSERT INTO actividad_empresa (id_empresa, ruc, razon_social, tipo, motivo, realizado_por, fecha)
SELECT e.id_empresa, e.ruc, e.razon_social, e.estado, e.motivo_revision, u.correo, e.fecha_revision
FROM empresa e
         LEFT JOIN usuario u ON u.id_usuario = e.id_admin_revisor
WHERE e.fecha_revision IS NOT NULL
  AND e.estado IN ('APROBADA', 'OBSERVADA', 'RECHAZADA');