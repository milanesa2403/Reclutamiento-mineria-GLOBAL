-- V3: Mejoras de integridad sobre el esquema existente. Es idempotente: se puede ejecutar sobre la BD actual de
-- Supabase o sobre una BD nueva. Si el equipo NO quiere estas mejoras, borren este archivo ANTES del primer arranque.

-- 1. es_principal: nunca NULL y por defecto FALSE.
--    (Antes el default era TRUE: cada especialidad nueva quedaba marcada como "principal".)
UPDATE especialidad_postulante SET es_principal = FALSE WHERE es_principal IS NULL;

-- Si un postulante ya tiene varias marcadas como principal, se conserva solo la primera (no se borra nada).
UPDATE especialidad_postulante e
SET es_principal = FALSE
WHERE e.es_principal
  AND EXISTS (SELECT 1 FROM especialidad_postulante o
              WHERE o.id_postulante = e.id_postulante
                AND o.es_principal
                AND o.id_especialidad < e.id_especialidad);

ALTER TABLE especialidad_postulante ALTER COLUMN es_principal SET DEFAULT FALSE;
ALTER TABLE especialidad_postulante ALTER COLUMN es_principal SET NOT NULL;

-- Como máximo UNA especialidad principal por postulante.
CREATE UNIQUE INDEX IF NOT EXISTS uq_especialidad_principal
    ON especialidad_postulante (id_postulante) WHERE es_principal;

-- 2. codigo_token debe ser único: se busca por ese valor al verificar el correo o restablecer la contraseña.
CREATE UNIQUE INDEX IF NOT EXISTS uq_token_codigo ON token_seguridad (codigo_token);
