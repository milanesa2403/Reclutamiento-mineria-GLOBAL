-- V14: amplía los catálogos de V10 (especialidades y habilidades del sector minero).
-- Idempotente: ON CONFLICT DO NOTHING (nombre es UNIQUE), se puede ejecutar sobre una BD que ya tenga datos.
-- IMPORTANTE: V10 no se edita (Flyway valida su checksum); todo valor nuevo va en una migración nueva.

INSERT INTO catalogo_especialidad (nombre, descripcion) VALUES
                                                            ('Perforación y Voladura', 'Diseño y ejecución de mallas de perforación y voladura controlada'),
                                                            ('Operación de Maquinaria Pesada', 'Operación de camiones, excavadoras, cargadores y tractores en mina'),
                                                            ('Procesamiento de Minerales', 'Operación de plantas de chancado, molienda y flotación'),
                                                            ('Planeamiento de Minas', 'Planificación de corto, mediano y largo plazo de la operación minera'),
                                                            ('Ventilación de Minas', 'Diseño y control de ventilación en operaciones subterráneas'),
                                                            ('Mantenimiento Eléctrico', 'Mantenimiento de sistemas eléctricos e instrumentación industrial'),
                                                            ('Medio Ambiente y Cierre de Minas', 'Gestión ambiental, monitoreo y planes de cierre'),
                                                            ('Geotecnia y Estabilidad de Taludes', 'Análisis geomecánico y control de estabilidad'),
                                                            ('Logística y Abastecimiento Minero', 'Gestión de almacenes, compras y transporte a operación'),
                                                            ('Salud Ocupacional', 'Vigilancia médica y prevención de enfermedades ocupacionales')
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO catalogo_habilidad (nombre, categoria) VALUES
                                                       ('Primeros Auxilios', 'Seguridad'),
                                                       ('Trabajos en Altura', 'Seguridad'),
                                                       ('Espacios Confinados', 'Seguridad'),
                                                       ('Manejo de Materiales Peligrosos', 'Seguridad'),
                                                       ('Permisos de Trabajo (PETAR)', 'Seguridad'),
                                                       ('Operación de Excavadora', 'Maquinaria'),
                                                       ('Operación de Camión Minero', 'Maquinaria'),
                                                       ('Operación de Cargador Frontal', 'Maquinaria'),
                                                       ('Operación de Jumbo', 'Maquinaria'),
                                                       ('Soldadura', 'Mantenimiento'),
                                                       ('Mantenimiento Hidráulico', 'Mantenimiento'),
                                                       ('Lectura de Planos Mecánicos', 'Mantenimiento'),
                                                       ('Muestreo Geológico', 'Geología'),
                                                       ('Logueo de Testigos', 'Geología'),
                                                       ('Modelamiento en Leapfrog', 'Software Técnico'),
                                                       ('Software Minero (Surpac / Vulcan)', 'Software Técnico'),
                                                       ('Excel Avanzado', 'Software Técnico'),
                                                       ('Manejo de GPS Diferencial', 'Topografía'),
                                                       ('Trabajo en Equipo', 'Competencias'),
                                                       ('Liderazgo de Cuadrillas', 'Competencias')
    ON CONFLICT (nombre) DO NOTHING;
