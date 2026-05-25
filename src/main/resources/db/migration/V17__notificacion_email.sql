CREATE TABLE notificacion_email (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID REFERENCES empresa (id),
    tipo VARCHAR(60) NOT NULL,
    destinatario_email VARCHAR(255) NOT NULL,
    destinatario_nombre VARCHAR(200),
    asunto VARCHAR(300) NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    provider_message_id VARCHAR(200),
    error_mensaje TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_envio TIMESTAMPTZ
);

CREATE INDEX idx_notificacion_email_empresa_fecha
ON notificacion_email (empresa_id, fecha_creacion DESC);

CREATE INDEX idx_notificacion_email_destinatario_fecha
ON notificacion_email (destinatario_email, fecha_creacion DESC);
