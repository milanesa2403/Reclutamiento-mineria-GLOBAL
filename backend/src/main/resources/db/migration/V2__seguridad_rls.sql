-- V2: Activa Row Level Security (RLS) en todas las tablas.
--
-- Por qué: Supabase expone por defecto las tablas del esquema "public" mediante su Data API (REST).
-- Con RLS activado y SIN políticas, esa API no puede leer ni escribir nada (por ejemplo, password_hash).
-- El backend Spring Boot no se ve afectado: se conecta con el usuario "postgres", que ignora RLS.
-- En un PostgreSQL local, el dueño de la tabla también ignora RLS, así que tampoco cambia nada.

ALTER TABLE usuario                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE token_seguridad         ENABLE ROW LEVEL SECURITY;
ALTER TABLE persona                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE empresa                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE postulante              ENABLE ROW LEVEL SECURITY;
ALTER TABLE representante_empresa   ENABLE ROW LEVEL SECURITY;
ALTER TABLE especialidad_postulante ENABLE ROW LEVEL SECURITY;
ALTER TABLE habilidad_postulante    ENABLE ROW LEVEL SECURITY;
ALTER TABLE certificacion           ENABLE ROW LEVEL SECURITY;
ALTER TABLE formacion_academica     ENABLE ROW LEVEL SECURITY;
ALTER TABLE experiencia_laboral     ENABLE ROW LEVEL SECURITY;
