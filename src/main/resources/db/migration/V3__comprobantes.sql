-- Comprobantes electrónicos (modelo unificado)

CREATE TABLE comprobante (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    tipo_codigo VARCHAR(2) NOT NULL,
    establecimiento_codigo VARCHAR(3) NOT NULL,
    punto_emision_codigo VARCHAR(3) NOT NULL,
    secuencial VARCHAR(9) NOT NULL,
    clave_acceso VARCHAR(49) NOT NULL UNIQUE,
    fecha_emision DATE NOT NULL,
    cliente_id UUID REFERENCES cliente (id),
    razon_social_receptor VARCHAR(300),
    identificacion_receptor VARCHAR(20),
    moneda VARCHAR(10) NOT NULL DEFAULT 'DOLAR',
    subtotal_sin_impuestos NUMERIC(14, 2),
    subtotal_0 NUMERIC(14, 2),
    subtotal_12 NUMERIC(14, 2),
    subtotal_no_objeto NUMERIC(14, 2),
    subtotal_exento NUMERIC(14, 2),
    descuento_total NUMERIC(14, 2),
    ice_total NUMERIC(14, 2),
    iva_total NUMERIC(14, 2),
    irbpnr_total NUMERIC(14, 2),
    propina NUMERIC(14, 2),
    valor_total NUMERIC(14, 2),
    estado_sri VARCHAR(30) NOT NULL DEFAULT 'GENERADO',
    numero_autorizacion VARCHAR(49),
    fecha_autorizacion TIMESTAMPTZ,
    ambiente_sri SMALLINT NOT NULL,
    tipo_emision SMALLINT NOT NULL,
    idempotency_key VARCHAR(100),
    origen VARCHAR(20),
    api_key_id UUID REFERENCES api_key (id),
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, idempotency_key),
    UNIQUE (empresa_id, tipo, establecimiento_codigo, punto_emision_codigo, secuencial)
);

CREATE INDEX idx_comprobante_emp_fecha ON comprobante (empresa_id, fecha_emision DESC);

CREATE INDEX idx_comprobante_estado ON comprobante (empresa_id, estado_sri);

CREATE TABLE comprobante_detalle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    linea INT NOT NULL,
    producto_id UUID REFERENCES producto (id),
    codigo_principal VARCHAR(50),
    codigo_auxiliar VARCHAR(50),
    descripcion VARCHAR(500),
    cantidad NUMERIC(14, 6),
    precio_unitario NUMERIC(14, 6),
    descuento NUMERIC(14, 2),
    precio_total_sin_impuesto NUMERIC(14, 2),
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE comprobante_impuesto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    detalle_id UUID REFERENCES comprobante_detalle (id) ON DELETE CASCADE,
    nivel VARCHAR(20),
    codigo VARCHAR(4),
    codigo_porcentaje VARCHAR(4),
    base_imponible NUMERIC(14, 2),
    valor NUMERIC(14, 2),
    tarifa NUMERIC(8, 2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE comprobante_retencion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    codigo VARCHAR(4),
    codigo_retencion VARCHAR(4),
    base_imponible NUMERIC(14, 2),
    porcentaje NUMERIC(8, 2),
    valor NUMERIC(14, 2),
    documento_sustento_tipo VARCHAR(2),
    documento_sustento_numero VARCHAR(20),
    documento_sustento_fecha DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE comprobante_guia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    direccion_partida VARCHAR(500),
    razon_social_transportista VARCHAR(300),
    tipo_id_transportista VARCHAR(2),
    ruc_transportista VARCHAR(13),
    placa VARCHAR(20),
    fecha_inicio_transporte DATE,
    fecha_fin_transporte DATE,
    motivo_traslado VARCHAR(500),
    direccion_destino VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE comprobante_estado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    mensaje TEXT,
    actor VARCHAR(100)
);

CREATE TABLE comprobante_archivo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    sha256 VARCHAR(64),
    tamano_bytes BIGINT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE comprobante_correo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID NOT NULL REFERENCES comprobante (id) ON DELETE CASCADE,
    destinatarios TEXT NOT NULL,
    asunto VARCHAR(300),
    cuerpo TEXT,
    estado VARCHAR(20),
    intentos INT NOT NULL DEFAULT 0,
    error TEXT,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE comprobante_log_sri (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprobante_id UUID REFERENCES comprobante (id),
    empresa_id UUID NOT NULL REFERENCES empresa (id),
    operacion VARCHAR(30),
    request TEXT,
    response TEXT,
    http_status INT,
    duracion_ms INT,
    error_codigo VARCHAR(20),
    error_mensaje TEXT,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE consumo_comprobante (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    paquete_id UUID REFERENCES paquete_comprobantes (id),
    comprobante_id UUID NOT NULL,
    tipo_comprobante VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consumo_emp_fecha ON consumo_comprobante (empresa_id, fecha DESC);
