-- V1: Esquema inicial. Es el esquema REAL que el equipo ya tiene creado en Supabase.
--
-- * En la BD existente de Supabase este archivo NO se ejecuta: Flyway la registra como "baseline" (versión 1)
--   y aplica solo V2 en adelante (ver spring.flyway.baseline-on-migrate en application.yml).
-- * En una BD vacía (por ejemplo, un PostgreSQL local) este archivo crea las tablas.
-- * No editar una vez que se haya ejecutado en alguna BD: los cambios van en una migración nueva (V4, V5...).

-- 1. AUTENTICACIÓN Y CONTROL DE ACCESO
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'EMPRESA', 'POSTULANTE')),
    cuenta_activa BOOLEAN NOT NULL DEFAULT FALSE, -- Se activa solo tras validar el correo
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. TOKENS Y CÓDIGOS DE SEGURIDAD: activación de cuentas y recuperación de contraseñas
CREATE TABLE IF NOT EXISTS token_seguridad (
    id_token BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    codigo_token VARCHAR(255) NOT NULL,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('ACTIVACION_CUENTA', 'RECUPERACION_PASSWORD')),
    fecha_expiracion TIMESTAMP NOT NULL,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. DATOS DE IDENTIDAD DE LA PERSONA NATURAL
CREATE TABLE IF NOT EXISTS persona (
    id_persona BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    dni VARCHAR(8) NOT NULL UNIQUE, -- DNI peruano de 8 dígitos
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    direccion VARCHAR(255)
);

-- 4. EMPRESA MINERA / PERSONA JURÍDICA
CREATE TABLE IF NOT EXISTS empresa (
    id_empresa BIGSERIAL PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE, -- RUC peruano de 11 dígitos
    razon_social VARCHAR(200) NOT NULL,
    sector_minero VARCHAR(100) NOT NULL, -- Ej: Tajo Abierto, Minería Subterránea, Polimetálica
    correo_corporativo VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. REPRESENTANTE DE LA EMPRESA (la persona que opera la cuenta de la empresa)
CREATE TABLE IF NOT EXISTS representante_empresa (
    id_representante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL UNIQUE REFERENCES persona(id_persona) ON DELETE CASCADE,
    id_empresa BIGINT NOT NULL REFERENCES empresa(id_empresa) ON DELETE CASCADE,
    cargo VARCHAR(100) NOT NULL DEFAULT 'Jefe de Selección / RRHH'
);

-- 6. PERFIL DEL POSTULANTE MINERO
CREATE TABLE IF NOT EXISTS postulante (
    id_postulante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL UNIQUE REFERENCES persona(id_persona) ON DELETE CASCADE,
    resumen_profesional TEXT,
    -- Rutas/URLs a archivos (Sprint 2; en Sprint 1 no se usan):
    url_cv TEXT,
    url_foto_dni TEXT,
    url_cul TEXT -- Certificado Único Laboral
);

-- 7. ESPECIALIDADES (1 a N con postulante)
CREATE TABLE IF NOT EXISTS especialidad_postulante (
    id_especialidad BIGSERIAL PRIMARY KEY,
    id_postulante BIGINT NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    nombre VARCHAR(120) NOT NULL, -- Ej: Operación de Maquinaria Pesada, SSOMA, u otra ingresada
    es_principal BOOLEAN DEFAULT TRUE -- (ajustado en V3)
);

-- 8. TABLAS COMPLEMENTARIAS DEL PERFIL PROFESIONAL
CREATE TABLE IF NOT EXISTS experiencia_laboral (
    id_experiencia BIGSERIAL PRIMARY KEY,
    id_postulante BIGINT NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    puesto VARCHAR(150) NOT NULL,
    empresa VARCHAR(150) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE, -- NULL si es el empleo actual
    descripcion TEXT
);

CREATE TABLE IF NOT EXISTS formacion_academica (
    id_formacion BIGSERIAL PRIMARY KEY,
    id_postulante BIGINT NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    institucion VARCHAR(200) NOT NULL,
    carrera_grado VARCHAR(150) NOT NULL, -- Ej: Tecsup, UNI, San Marcos
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE
);

CREATE TABLE IF NOT EXISTS certificacion (
    id_certificacion BIGSERIAL PRIMARY KEY,
    id_postulante BIGINT NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    nombre VARCHAR(200) NOT NULL, -- Ej: Anexo 6 SSOMA, Manejo Defensivo en Mina 4x4
    entidad_emisora VARCHAR(150) NOT NULL,
    fecha_emision DATE
);

CREATE TABLE IF NOT EXISTS habilidad_postulante (
    id_habilidad BIGSERIAL PRIMARY KEY,
    id_postulante BIGINT NOT NULL REFERENCES postulante(id_postulante) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL -- Ej: Perforación Jumbo, Primeros Auxilios, Liderazgo
);
