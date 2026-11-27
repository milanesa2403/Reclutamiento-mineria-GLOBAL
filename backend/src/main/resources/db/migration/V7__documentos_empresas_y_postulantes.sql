-- V7: Documentos (tabla única + bucket privado), aprobación de empresas (H008/HU004), PDF de certificaciones,
--     documentos del perfil del postulante e invalidación de sesiones (P13).
--     Consolida lo que antes eran V7, V9 y V10 (nunca se aplicaron en Supabase).
--
-- Compatibilidad con el resto del equipo:
--  * Solo AGREGA columnas/tablas, salvo la sección 5 (elimina postulante_certificado, tabla manual con 0 filas).
--    Hibernate (ddl-auto=validate) no se queja de columnas que sobran.
--  * Las empresas que YA existen quedan APROBADA (no se bloquea a nadie). Las nuevas nacen PENDIENTE_DOCUMENTO
--    (también si las inserta código de otra rama que no conoce esta columna: lo pone el DEFAULT).
--  * Idempotente. No editar después de ejecutarse (Flyway guarda su checksum): los cambios van en una migración nueva.

-- 1. Tabla ÚNICA de documentos subidos (Supabase Storage, bucket PRIVADO): acreditación de empresa, logo, banner y
--    PDF de certificaciones. (CV, foto del DNI, CUL y EMO del postulante van en postulante.url_*: sección 5.)
CREATE TABLE IF NOT EXISTS documento (
    id_documento         BIGSERIAL PRIMARY KEY,
    id_usuario           BIGINT       NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE, -- dueño
    tipo                 VARCHAR(30)  NOT NULL CHECK (tipo IN ('ACREDITACION_EMPRESA', 'CV', 'FOTO_DNI', 'CUL', 'EMO',
                                                                'CERTIFICADO', 'IMAGEN_EMPRESA', 'BANNER_EMPRESA')),
    nombre_original      VARCHAR(255) NOT NULL,          -- solo para mostrar; nunca se usa como ruta
    tipo_mime            VARCHAR(100) NOT NULL,          -- detectado por el CONTENIDO del archivo, no por la extensión
    tamano_bytes         BIGINT       NOT NULL CHECK (tamano_bytes > 0),
    hash_sha256          VARCHAR(64)  NOT NULL,
    clave_almacenamiento VARCHAR(500) NOT NULL UNIQUE,   -- ruta dentro del bucket (generada con UUID)
    fecha_subida         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_documento_usuario_tipo ON documento (id_usuario, tipo);
ALTER TABLE documento ENABLE ROW LEVEL SECURITY;

-- 2. Estado de la solicitud de registro de la empresa.
--    PENDIENTE_DOCUMENTO → (sube acreditación) → EN_REVISION → APROBADA | OBSERVADA | RECHAZADA
--    OBSERVADA → (sube un documento nuevo) → EN_REVISION
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'APROBADA'; -- filas existentes
ALTER TABLE empresa ALTER COLUMN estado SET DEFAULT 'PENDIENTE_DOCUMENTO';                    -- filas nuevas
ALTER TABLE empresa DROP CONSTRAINT IF EXISTS ck_empresa_estado;
ALTER TABLE empresa ADD CONSTRAINT ck_empresa_estado
    CHECK (estado IN ('PENDIENTE_DOCUMENTO', 'EN_REVISION', 'OBSERVADA', 'APROBADA', 'RECHAZADA'));

ALTER TABLE empresa ADD COLUMN IF NOT EXISTS motivo_revision VARCHAR(500);  -- obligatorio al observar o rechazar
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS fecha_revision TIMESTAMP;      -- última decisión del administrador
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS id_admin_revisor BIGINT
    REFERENCES usuario(id_usuario) ON DELETE SET NULL;
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS id_documento_acreditacion BIGINT
    REFERENCES documento(id_documento) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_empresa_estado ON empresa (estado);

-- 3. Rastro mínimo de las empresas rechazadas cuyos datos se eliminaron pasados N días (sin datos personales:
--    ni correo, ni DNI, ni nombres). Permite responder "¿por qué se rechazó este RUC?" y detectar reintentos.
CREATE TABLE IF NOT EXISTS empresa_rechazo_historial (
    id_historial  BIGSERIAL PRIMARY KEY,
    ruc           VARCHAR(11)  NOT NULL,
    razon_social  VARCHAR(200) NOT NULL,
    motivo        VARCHAR(500) NOT NULL,
    fecha_rechazo TIMESTAMP    NOT NULL,
    fecha_purga   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_empresa_rechazo_ruc ON empresa_rechazo_historial (ruc);
ALTER TABLE empresa_rechazo_historial ENABLE ROW LEVEL SECURITY;

-- 4. PDF de cada certificación del postulante (HU006/HU010/HU014).
--    La certificación (nombre, entidad, fecha) existe desde V1; su archivo va en "documento" (tipo CERTIFICADO).
--    SET NULL: si se elimina el archivo, la certificación se conserva (sin PDF).
ALTER TABLE certificacion
    ADD COLUMN IF NOT EXISTS id_documento BIGINT UNIQUE REFERENCES documento(id_documento) ON DELETE SET NULL;

-- 5. Documentos del perfil del postulante (CV, foto del DNI, CUL, EMO): sus columnas url_* guardan la RUTA del archivo
--    vigente en el bucket PRIVADO (no una URL pública); para verlo, el backend genera una URL firmada que caduca.
--    postulante.url_emo ya existe en Supabase (se agregó a mano, fuera de Flyway): aquí se formaliza (IF NOT EXISTS).
ALTER TABLE postulante ADD COLUMN IF NOT EXISTS url_emo TEXT;

--    postulante_certificado (creada a mano en Supabase, 0 filas verificadas por el equipo el 06/10/2026) se reemplaza
--    por certificacion + documento (sección 4). Sin CASCADE: si algo dependiera de ella, la migración falla en vez de
--    borrar más de la cuenta.
DROP TABLE IF EXISTS postulante_certificado;

-- 6. P13: momento del último cambio de contraseña (UTC). Los JWT emitidos ANTES dejan de ser válidos.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_cambio_password TIMESTAMP;
