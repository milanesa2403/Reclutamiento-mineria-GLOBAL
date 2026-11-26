-- V13: Vacantes por convocatoria y "búsquedas" (carpetas opcionales para organizar convocatorias).
--
-- 1. convocatoria.vacantes: cuántas personas se contratan para ese puesto. Las filas existentes quedan con 1.
-- 2. busqueda: carpeta de la empresa que agrupa convocatorias de una misma campaña (p. ej. "Parada octubre 2026").
--    Solo organiza: cada convocatoria sigue teniendo TODOS sus datos (ubicación, fechas, requisitos...). No hay datos
--    heredados; "agregar puesto" en el frontend solo precarga el formulario copiando los de otro puesto.
-- 3. convocatoria.id_busqueda: opcional. NULL = convocatoria suelta. Al borrar la carpeta, sus convocatorias quedan
--    sueltas (no se pierde nada). Que la carpeta sea de la misma empresa lo valida el backend.
--
-- Las postulaciones no cambian: se sigue postulando a cada convocatoria (puesto) por separado.

-- 1. Vacantes -------------------------------------------------------------------------------------------------------
ALTER TABLE convocatoria
    ADD COLUMN vacantes SMALLINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_convocatoria_vacantes CHECK (vacantes BETWEEN 1 AND 999);

-- 2. Búsquedas (carpetas) -------------------------------------------------------------------------------------------
CREATE TABLE busqueda (
    id_busqueda    BIGSERIAL    PRIMARY KEY,
    id_empresa     BIGINT       NOT NULL REFERENCES empresa(id_empresa) ON DELETE CASCADE,
    nombre         VARCHAR(120) NOT NULL CHECK (btrim(nombre) <> ''),
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_busqueda_nombre ON busqueda (id_empresa, lower(nombre));
ALTER TABLE busqueda ENABLE ROW LEVEL SECURITY;

-- 3. Convocatoria dentro de una búsqueda (opcional) -----------------------------------------------------------------
ALTER TABLE convocatoria
    ADD COLUMN id_busqueda BIGINT REFERENCES busqueda(id_busqueda) ON DELETE SET NULL;
CREATE INDEX ix_convocatoria_busqueda ON convocatoria (id_busqueda);
