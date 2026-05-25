ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS nombre_comercial VARCHAR(300),
    ADD COLUMN IF NOT EXISTS tipo_tercero VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    ADD COLUMN IF NOT EXISTS contacto_nombre VARCHAR(200),
    ADD COLUMN IF NOT EXISTS contacto_telefono VARCHAR(50),
    ADD COLUMN IF NOT EXISTS contacto_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS obligado_contabilidad VARCHAR(2),
    ADD COLUMN IF NOT EXISTS contribuyente_especial VARCHAR(50),
    ADD COLUMN IF NOT EXISTS regimen VARCHAR(100),
    ADD COLUMN IF NOT EXISTS estado_sri VARCHAR(50),
    ADD COLUMN IF NOT EXISTS actividad_economica VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS fuente_datos VARCHAR(30);

CREATE TABLE IF NOT EXISTS cliente_direccion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente (id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL DEFAULT 'MATRIZ',
    direccion VARCHAR(500) NOT NULL,
    provincia VARCHAR(120),
    canton VARCHAR(120),
    parroquia VARCHAR(120),
    referencia VARCHAR(300),
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cliente_tipo_tercero ON cliente (empresa_id, tipo_tercero);
CREATE INDEX IF NOT EXISTS idx_cliente_direccion_cliente ON cliente_direccion (cliente_id);
