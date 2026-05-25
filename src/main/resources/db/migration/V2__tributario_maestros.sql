-- Configuración tributaria y maestros

CREATE TABLE certificado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    alias VARCHAR(150),
    archivo_storage_key VARCHAR(500) NOT NULL,
    password_cifrado TEXT NOT NULL,
    emisor VARCHAR(300),
    serial VARCHAR(100),
    valido_desde TIMESTAMPTZ,
    valido_hasta TIMESTAMPTZ,
    activo_para_firma BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE establecimiento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),
    direccion VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE punto_emision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    establecimiento_id UUID NOT NULL REFERENCES establecimiento (id) ON DELETE CASCADE,
    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (establecimiento_id, codigo)
);

CREATE TABLE secuencial (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    punto_emision_id UUID NOT NULL REFERENCES punto_emision (id) ON DELETE CASCADE,
    tipo_comprobante VARCHAR(2) NOT NULL,
    valor_actual BIGINT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (punto_emision_id, tipo_comprobante)
);

CREATE TABLE cliente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    tipo_identificacion VARCHAR(2) NOT NULL,
    identificacion VARCHAR(20) NOT NULL,
    razon_social VARCHAR(300) NOT NULL,
    direccion VARCHAR(500),
    telefono VARCHAR(50),
    email VARCHAR(255),
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, tipo_identificacion, identificacion)
);

CREATE TABLE producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    codigo_principal VARCHAR(50) NOT NULL,
    codigo_auxiliar VARCHAR(50),
    descripcion VARCHAR(500) NOT NULL,
    tipo VARCHAR(20),
    precio_unitario NUMERIC(14, 6),
    iva_codigo VARCHAR(4),
    ice_codigo VARCHAR(10),
    irbpnr_codigo VARCHAR(10),
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, codigo_principal)
);

CREATE INDEX idx_cliente_empresa ON cliente (empresa_id);

CREATE INDEX idx_producto_empresa ON producto (empresa_id);
