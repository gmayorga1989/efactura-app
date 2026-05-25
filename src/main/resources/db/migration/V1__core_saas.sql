-- Core SaaS: empresas, seguridad comercial, planes (Fase 1 base)

CREATE TABLE empresa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ruc VARCHAR(13) NOT NULL UNIQUE,
    razon_social VARCHAR(300) NOT NULL,
    nombre_comercial VARCHAR(300),
    obligado_contabilidad BOOLEAN NOT NULL DEFAULT FALSE,
    contribuyente_especial VARCHAR(20),
    ambiente_sri SMALLINT NOT NULL DEFAULT 1,
    tipo_emision SMALLINT NOT NULL DEFAULT 1,
    direccion_matriz VARCHAR(500),
    logo_url VARCHAR(500),
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Guayaquil',
    config_extra JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    precio_mensual NUMERIC(12, 2),
    precio_anual NUMERIC(12, 2),
    max_usuarios INT,
    max_establecimientos INT,
    incluye_api BOOLEAN NOT NULL DEFAULT FALSE,
    incluye_scraper BOOLEAN NOT NULL DEFAULT FALSE,
    caracteristicas JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(300),
    modulo VARCHAR(50),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    sistema BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE rol_permiso (
    rol_id UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    permiso_id UUID NOT NULL REFERENCES permiso (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    mfa_secret VARCHAR(100),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login TIMESTAMPTZ,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, email)
);

CREATE TABLE usuario_rol (
    usuario_id UUID NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE suscripcion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plan (id),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    facturacion VARCHAR(20) NOT NULL DEFAULT 'MENSUAL',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    auto_renovar BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE paquete_comprobantes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    nombre VARCHAR(150) NOT NULL,
    cantidad INT NOT NULL,
    cantidad_consumida INT NOT NULL DEFAULT 0,
    fecha_compra TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_vencimiento TIMESTAMPTZ,
    aplica_tipos JSONB,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    nombre VARCHAR(150),
    prefix VARCHAR(32) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL,
    scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
    rate_limit_rpm INT NOT NULL DEFAULT 60,
    ip_whitelist JSONB,
    ultimo_uso TIMESTAMPTZ,
    fecha_expiracion TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id),
    usuario VARCHAR(150),
    api_key_id UUID REFERENCES api_key (id),
    accion VARCHAR(100) NOT NULL,
    entidad VARCHAR(100),
    entidad_id UUID,
    cambios JSONB,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_empresa_fecha ON auditoria (empresa_id, fecha DESC);

INSERT INTO plan (codigo, nombre, precio_mensual, max_usuarios, incluye_api, caracteristicas)
VALUES (
        'STARTER',
        'Starter',
        29.99,
        3,
        TRUE,
        '{"comprobantes_mes": 500}'::jsonb
    );

INSERT INTO permiso (codigo, descripcion, modulo)
VALUES ('EMPRESA_ADMIN', 'Administración de empresa', 'CORE'),
    ('FACTURA_EMITIR', 'Emitir facturas', 'EMISION'),
    ('REPORTE_VER', 'Ver reportes', 'REPORTES');
