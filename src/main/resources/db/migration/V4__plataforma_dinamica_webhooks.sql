-- Plataforma dinámica, webhooks, idempotencia, SRI recibidos

CREATE TABLE custom_field_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE,
    entidad VARCHAR(50) NOT NULL,
    tipo_documento VARCHAR(30),
    codigo VARCHAR(80) NOT NULL,
    etiqueta VARCHAR(150) NOT NULL,
    tipo_dato VARCHAR(20) NOT NULL,
    longitud INT,
    expresion_regex VARCHAR(500),
    obligatorio BOOLEAN NOT NULL DEFAULT FALSE,
    visible_ui BOOLEAN NOT NULL DEFAULT TRUE,
    imprimir_ride BOOLEAN NOT NULL DEFAULT FALSE,
    enviar_xml_info_adicional BOOLEAN NOT NULL DEFAULT FALSE,
    usar_reporteria BOOLEAN NOT NULL DEFAULT FALSE,
    grupo VARCHAR(80),
    orden INT NOT NULL DEFAULT 0,
    valor_default VARCHAR(500),
    ayuda VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, entidad, tipo_documento, codigo)
);

CREATE TABLE custom_field_option (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES custom_field_definition (id) ON DELETE CASCADE,
    valor VARCHAR(200) NOT NULL,
    etiqueta VARCHAR(200),
    orden INT
);

CREATE TABLE custom_field_value (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    field_id UUID NOT NULL REFERENCES custom_field_definition (id) ON DELETE CASCADE,
    entidad VARCHAR(50) NOT NULL,
    entidad_id UUID NOT NULL,
    valor_texto TEXT,
    valor_numero NUMERIC(20, 6),
    valor_fecha TIMESTAMPTZ,
    valor_bool BOOLEAN,
    UNIQUE (field_id, entidad, entidad_id)
);

CREATE INDEX idx_cfv_emp_field_text ON custom_field_value (empresa_id, field_id, valor_texto);

CREATE TABLE business_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE,
    tipo_documento VARCHAR(30),
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    cuando JSONB NOT NULL,
    entonces JSONB NOT NULL,
    mensaje VARCHAR(500),
    severidad VARCHAR(20) NOT NULL DEFAULT 'ERROR',
    prioridad INT NOT NULL DEFAULT 100,
    vigente_desde TIMESTAMPTZ,
    vigente_hasta TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE ride_template_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    tipo_documento VARCHAR(30),
    nombre VARCHAR(150),
    motor VARCHAR(20) NOT NULL DEFAULT 'JASPER',
    template_storage_key VARCHAR(500),
    parametros JSONB NOT NULL DEFAULT '{}'::jsonb,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE webhook (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    eventos JSONB NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE webhook_envio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID NOT NULL REFERENCES webhook (id) ON DELETE CASCADE,
    evento VARCHAR(50),
    payload JSONB,
    response_status INT,
    response_body TEXT,
    intentos INT NOT NULL DEFAULT 0,
    estado VARCHAR(20),
    proximo_intento TIMESTAMPTZ,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sri_comprobante_recibido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    clave_acceso VARCHAR(49) NOT NULL,
    tipo_comprobante VARCHAR(2),
    ruc_emisor VARCHAR(13),
    razon_social_emisor VARCHAR(300),
    fecha_emision DATE,
    fecha_autorizacion TIMESTAMPTZ,
    valor_total NUMERIC(14, 2),
    xml_storage_key VARCHAR(500),
    procesado BOOLEAN NOT NULL DEFAULT FALSE,
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, clave_acceso)
);

CREATE TABLE sri_descarga_programada (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    cron VARCHAR(50),
    rango_dias INT NOT NULL DEFAULT 1,
    ultima_ejecucion TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE sri_bot_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    tipo_operacion VARCHAR(50),
    estado VARCHAR(20),
    mensaje TEXT,
    duracion_ms INT,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE idempotency_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(200) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    UNIQUE (empresa_id, idempotency_key)
);

CREATE INDEX idx_idempotency_expires ON idempotency_request (expires_at);
