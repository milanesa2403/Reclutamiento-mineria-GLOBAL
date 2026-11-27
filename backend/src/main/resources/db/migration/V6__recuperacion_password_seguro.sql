-- V6: Recuperación de contraseña segura (HU003).
--   1. Límite de intentos por código: columna token_seguridad.intentos.
--   2. Un solo código de recuperación vigente por usuario.
--   3. Se reemplaza el índice único GLOBAL uq_token_codigo (V3). Con códigos de 6 dígitos que nunca se borraban,
--      las colisiones entre usuarios distintos daban errores 500 cada vez más seguido. Ahora la unicidad es:
--        - tokens de ACTIVACION (64 hex aleatorios): únicos entre sí, como antes.
--        - tokens de RECUPERACION: máximo uno vigente (no usado) por usuario. Dos usuarios SÍ pueden tener el mismo
--          código de 6 dígitos, porque la búsqueda ahora siempre es por usuario + código.
-- Idempotente. No editar después de ejecutarse (Flyway guarda su checksum): los cambios van en una V7.

-- 1. Intentos fallidos (o de verificación) consumidos por este código.
ALTER TABLE token_seguridad ADD COLUMN IF NOT EXISTS intentos INTEGER NOT NULL DEFAULT 0;

-- 2. Si hoy hay varios códigos de recuperación vigentes para un mismo usuario, se conserva solo el más reciente.
--    (Imprescindible antes de crear el índice único parcial de abajo, o el índice fallaría.)
UPDATE token_seguridad t
SET utilizado = TRUE
WHERE t.tipo = 'RECUPERACION_PASSWORD'
  AND t.utilizado = FALSE
  AND EXISTS (SELECT 1 FROM token_seguridad o
              WHERE o.id_usuario = t.id_usuario
                AND o.tipo = 'RECUPERACION_PASSWORD'
                AND o.utilizado = FALSE
                AND o.id_token > t.id_token);

-- 3. Nuevos índices de unicidad.
DROP INDEX IF EXISTS uq_token_codigo;

CREATE UNIQUE INDEX IF NOT EXISTS uq_token_activacion
    ON token_seguridad (codigo_token) WHERE tipo = 'ACTIVACION_CUENTA';

CREATE UNIQUE INDEX IF NOT EXISTS uq_token_recuperacion_vigente
    ON token_seguridad (id_usuario) WHERE tipo = 'RECUPERACION_PASSWORD' AND utilizado = FALSE;

-- Búsquedas por usuario + tipo (las que hace ahora la recuperación).
CREATE INDEX IF NOT EXISTS ix_token_usuario_tipo ON token_seguridad (id_usuario, tipo);