-- V4: El sector minero pasa de texto libre a un CATÁLOGO (lista de opciones).
--     Antes: empresa.sector_minero VARCHAR(100)  ->  Ahora: empresa.id_sector_minero (FK a sector_minero).
--
-- ANTES DEL PRIMER ARRANQUE: revisen y ajusten la lista de abajo con su criterio del negocio.
-- Después de ejecutarse, esta migración NO se edita: los cambios van en una migración nueva.
-- (Agregar o desactivar sectores más adelante es solo un INSERT / UPDATE de datos.)

CREATE TABLE IF NOT EXISTS sector_minero (
    id_sector_minero BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE  -- en vez de borrar un sector en uso, se desactiva
);

-- Igual que las demás tablas (ver V2): la API pública de Supabase no debe poder leer ni escribir aquí.
ALTER TABLE sector_minero ENABLE ROW LEVEL SECURITY;

-- Lista inicial (el orden de inserción es el orden en que se muestra en el formulario; "Otro" va al final).
INSERT INTO sector_minero (nombre) VALUES
    ('Minería a tajo abierto'),
    ('Minería subterránea'),
    ('Minería polimetálica'),
    ('Minería no metálica'),
    ('Exploración minera'),
    ('Servicios y contratistas mineros'),
    ('Proveedores de equipos e insumos mineros'),
    ('Otro')
ON CONFLICT (nombre) DO NOTHING;

-- Conservar los datos que ya existan: cualquier texto libre que hubiera en empresa.sector_minero
-- se agrega al catálogo tal cual (si no coincide con la lista de arriba, quedará como una opción más que se puede limpiar).
INSERT INTO sector_minero (nombre)
SELECT DISTINCT TRIM(sector_minero)
FROM empresa
WHERE sector_minero IS NOT NULL AND TRIM(sector_minero) <> ''
ON CONFLICT (nombre) DO NOTHING;

-- Nueva columna con clave foránea.
-- Sin ON DELETE CASCADE a propósito: no se puede borrar un sector que una empresa está usando.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS id_sector_minero BIGINT REFERENCES sector_minero(id_sector_minero);

UPDATE empresa e
SET id_sector_minero = s.id_sector_minero
FROM sector_minero s
WHERE s.nombre = TRIM(e.sector_minero)
  AND e.id_sector_minero IS NULL;

ALTER TABLE empresa ALTER COLUMN id_sector_minero SET NOT NULL;
ALTER TABLE empresa DROP COLUMN sector_minero;

CREATE INDEX IF NOT EXISTS idx_empresa_sector_minero ON empresa (id_sector_minero);
