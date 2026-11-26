-- V8: Convocatorias (base para HU012–HU015). Diseño aprobado: PROPUESTA_CONVOCATORIAS_Y_CERTIFICADOS.md.
--
--  * Toda convocatoria pertenece a UNA empresa (relación obligatoria). La empresa sale del JWT, nunca del cliente.
--  * id_usuario_creador / id_usuario_publicador: SOLO trazabilidad (quién la creó / quién la publicó). No se usan para
--    permisos: los roles de usuarios de empresa son de otra historia (HU005/HU011). SET NULL: si se elimina el usuario,
--    la convocatoria sigue siendo de la empresa. La vista pública muestra la empresa, no a estas personas.
--  * tipo: valor cerrado PLANILLA_COMPLETA | PARADA_PLANTA (no texto libre). Lo propio de una parada de planta (su
--    periodo de trabajo) son dos columnas que el CHECK exige solo en ese tipo. Si a futuro cada tipo acumula muchos
--    datos propios, se mueven a una tabla 1:1 por tipo sin tocar el resto.
--  * Un solo estado (BORRADOR → PUBLICADA → CERRADA): reemplaza al par "activo + estado" que podía contradecirse.
--  * La tabla "convocatoria" creada a mano en Supabase fue eliminada por el equipo antes de esta migración.
--  * Las postulaciones (HU014/HU015) van en su propia migración, junto con esa historia.

CREATE TABLE convocatoria (
    id_convocatoria          BIGSERIAL    PRIMARY KEY,
    id_empresa               BIGINT       NOT NULL REFERENCES empresa(id_empresa) ON DELETE CASCADE,
    id_usuario_creador       BIGINT       REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    id_usuario_publicador    BIGINT       REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    tipo                     VARCHAR(20)  NOT NULL CHECK (tipo IN ('PLANILLA_COMPLETA', 'PARADA_PLANTA')),
    puesto                   VARCHAR(150) NOT NULL,
    campamento               VARCHAR(150) NOT NULL,          -- ubicación / unidad minera
    turno                    VARCHAR(50)  NOT NULL,          -- p. ej. "14x7", "20x10", "Lunes a sábado"
    descripcion              TEXT         NOT NULL,
    requisitos_obligatorios  TEXT         NOT NULL,          -- HU015 filtra por lo obligatorio
    requisitos_deseables     TEXT,
    experiencia_minima_anios SMALLINT     CHECK (experiencia_minima_anios BETWEEN 0 AND 50),
    fecha_inicio_labores     DATE,                           -- obligatorio en PARADA_PLANTA
    fecha_fin_labores        DATE,                           -- obligatorio en PARADA_PLANTA
    estado                   VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR'
                             CHECK (estado IN ('BORRADOR', 'PUBLICADA', 'CERRADA')),
    fecha_creacion           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_publicacion        TIMESTAMP,
    fecha_cierre             DATE,                           -- último día para postular (opcional)
    CONSTRAINT ck_convocatoria_publicada CHECK (estado = 'BORRADOR' OR fecha_publicacion IS NOT NULL),
    CONSTRAINT ck_convocatoria_parada CHECK (tipo <> 'PARADA_PLANTA'
        OR (fecha_inicio_labores IS NOT NULL AND fecha_fin_labores IS NOT NULL
            AND fecha_fin_labores >= fecha_inicio_labores))
);

CREATE INDEX ix_convocatoria_feed ON convocatoria (estado, fecha_publicacion DESC);   -- HU013: feed de publicadas
CREATE INDEX ix_convocatoria_empresa ON convocatoria (id_empresa);                    -- HU012: "mis convocatorias"

-- Igual que el resto de tablas (V2/V7): sin políticas = nadie accede por la API pública de Supabase; solo el backend.
ALTER TABLE convocatoria ENABLE ROW LEVEL SECURITY;
