ALTER TABLE persona ADD COLUMN pais VARCHAR(2) NOT NULL DEFAULT 'PE';
ALTER TABLE persona ADD COLUMN tipo_documento VARCHAR(20) NOT NULL DEFAULT 'DNI';
ALTER TABLE persona ALTER COLUMN dni TYPE VARCHAR(20);
ALTER TABLE persona DROP CONSTRAINT IF EXISTS persona_dni_key;
ALTER TABLE persona ADD CONSTRAINT uq_persona_documento UNIQUE (pais, tipo_documento, dni);
ALTER TABLE persona ADD CONSTRAINT ck_persona_tipo_documento
    CHECK (tipo_documento IN ('DNI', 'PASAPORTE', 'CARNE_EXTRANJERIA', 'CEDULA', 'OTRO'));

ALTER TABLE empresa ADD COLUMN pais VARCHAR(2) NOT NULL DEFAULT 'PE';
ALTER TABLE empresa ALTER COLUMN ruc TYPE VARCHAR(20);
ALTER TABLE empresa DROP CONSTRAINT IF EXISTS empresa_ruc_key;
ALTER TABLE empresa ADD CONSTRAINT uq_empresa_identificador UNIQUE (pais, ruc);

ALTER TABLE actividad_empresa ALTER COLUMN ruc TYPE VARCHAR(20);
ALTER TABLE empresa_rechazo_historial ALTER COLUMN ruc TYPE VARCHAR(20);
