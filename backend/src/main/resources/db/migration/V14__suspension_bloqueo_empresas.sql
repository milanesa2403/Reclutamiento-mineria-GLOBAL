-- V14: estados SUSPENDIDA y BLOQUEADA para empresas + nuevos tipos de evento en la auditoría.
--
--   APROBADA <-> SUSPENDIDA            (temporal, reversible)
--   APROBADA | SUSPENDIDA -> BLOQUEADA (definitivo)
--
-- BLOQUEADA no la borra la purga (solo purga RECHAZADA): el RUC y el DNI de los representantes siguen ocupados,
-- así que existsByRuc / existsDniRepresentante impiden volver a registrarse sin código extra.
-- La ADVERTENCIA es solo un evento de auditoría: no cambia el estado de la empresa.

ALTER TABLE empresa DROP CONSTRAINT IF EXISTS ck_empresa_estado;
ALTER TABLE empresa ADD CONSTRAINT ck_empresa_estado
    CHECK (estado IN ('PENDIENTE_DOCUMENTO', 'EN_REVISION', 'OBSERVADA', 'APROBADA',
                      'SUSPENDIDA', 'BLOQUEADA', 'RECHAZADA'));

-- El CHECK de V11 se creó sin nombre: PostgreSQL lo llamó actividad_empresa_tipo_check.
ALTER TABLE actividad_empresa DROP CONSTRAINT IF EXISTS actividad_empresa_tipo_check;
ALTER TABLE actividad_empresa DROP CONSTRAINT IF EXISTS ck_actividad_empresa_tipo;
ALTER TABLE actividad_empresa ADD CONSTRAINT ck_actividad_empresa_tipo
    CHECK (tipo IN ('REGISTRADA', 'DOCUMENTO_ENVIADO', 'APROBADA', 'OBSERVADA', 'RECHAZADA',
                    'ADVERTENCIA', 'SUSPENDIDA', 'REACTIVADA', 'BLOQUEADA'));