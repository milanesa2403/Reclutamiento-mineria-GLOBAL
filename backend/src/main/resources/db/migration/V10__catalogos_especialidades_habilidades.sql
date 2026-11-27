-- V10: Catálogos predefinidos para especialidades y habilidades del sector minero

CREATE TABLE IF NOT EXISTS catalogo_especialidad (
                                                     id SERIAL PRIMARY KEY,
                                                     nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion TEXT
    );

CREATE TABLE IF NOT EXISTS catalogo_habilidad (
                                                  id SERIAL PRIMARY KEY,
                                                  nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(50)
    );

-- Insertar valores iniciales de ejemplo para el sector minero
INSERT INTO catalogo_especialidad (nombre, descripcion) VALUES
                                                            ('Geología de Minas', 'Exploración y modelamiento geológico en operaciones mineras'),
                                                            ('Mantenimiento Mecánico', 'Gestión y reparación de maquinaria pesada y plantas concentradoras'),
                                                            ('Seguridad Minera y OHSAS', 'Gestión de riesgos, prevención de pérdidas y seguridad industrial'),
                                                            ('Topografía y Geomensura', 'Levantamientos topográficos subterráneos y de superficie')
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO catalogo_habilidad (nombre, categoria) VALUES
                                                       ('AutoCAD Civil 3D', 'Software Técnico'),
                                                       ('Operación de Perforadoras', 'Maquinaria'),
                                                       ('Gestión de Riesgos (IPERC)', 'Seguridad'),
                                                       ('Manejo de Estación Total', 'Topografía')
    ON CONFLICT (nombre) DO NOTHING;