-- Proformas / cotizaciones y vendedores

CREATE TABLE vendedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    codigo VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120),
    email VARCHAR(255),
    telefono VARCHAR(30),
    documento_identidad VARCHAR(20),
    notas VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_vendedor_empresa ON vendedor (empresa_id, estado);

CREATE TABLE vendedor_meta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id UUID NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    periodo_anio INT NOT NULL,
    periodo_mes INT NOT NULL,
    meta_monto NUMERIC(14, 2) NOT NULL DEFAULT 0,
    meta_cantidad INT,
    notas VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    UNIQUE (vendedor_id, periodo_anio, periodo_mes)
);

CREATE TABLE cotizacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    numero VARCHAR(30) NOT NULL,
    fecha_emision DATE NOT NULL,
    validez_dias INT NOT NULL DEFAULT 15,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    cliente_id UUID REFERENCES cliente (id),
    vendedor_id UUID REFERENCES vendedor (id),
    tipo_identificacion_receptor VARCHAR(2),
    identificacion_receptor VARCHAR(20),
    razon_social_receptor VARCHAR(300),
    email_receptor VARCHAR(255),
    moneda VARCHAR(10) NOT NULL DEFAULT 'DOLAR',
    subtotal_sin_impuestos NUMERIC(14, 2),
    descuento_total NUMERIC(14, 2),
    iva_total NUMERIC(14, 2),
    valor_total NUMERIC(14, 2),
    introduccion_html TEXT,
    condiciones_html TEXT,
    plantilla_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    comprobante_id UUID REFERENCES comprobante (id),
    fecha_envio TIMESTAMPTZ,
    fecha_conversion TIMESTAMPTZ,
    estado_registro VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, numero)
);

CREATE INDEX idx_cotizacion_emp_fecha ON cotizacion (empresa_id, fecha_emision DESC);
CREATE INDEX idx_cotizacion_vendedor ON cotizacion (empresa_id, vendedor_id);
CREATE INDEX idx_cotizacion_estado ON cotizacion (empresa_id, estado);

CREATE TABLE cotizacion_detalle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cotizacion_id UUID NOT NULL REFERENCES cotizacion (id) ON DELETE CASCADE,
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    linea INT NOT NULL,
    producto_id UUID REFERENCES producto (id),
    codigo_principal VARCHAR(50),
    codigo_auxiliar VARCHAR(50),
    descripcion VARCHAR(500) NOT NULL,
    cantidad NUMERIC(14, 6) NOT NULL,
    precio_unitario NUMERIC(14, 6) NOT NULL,
    descuento NUMERIC(14, 2) DEFAULT 0,
    iva_porcentaje NUMERIC(8, 2),
    precio_total_sin_impuesto NUMERIC(14, 2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cotizacion_adjunto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cotizacion_id UUID NOT NULL REFERENCES cotizacion (id) ON DELETE CASCADE,
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL DEFAULT 'ENLACE',
    proveedor VARCHAR(30),
    titulo VARCHAR(200),
    url VARCHAR(2000) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE comprobante ADD COLUMN IF NOT EXISTS vendedor_id UUID REFERENCES vendedor (id);
CREATE INDEX IF NOT EXISTS idx_comprobante_vendedor ON comprobante (empresa_id, vendedor_id);

-- Plantilla por defecto de cotización en configuración de empresa (clave en custom_data vía app)
COMMENT ON COLUMN cotizacion.plantilla_json IS 'Diseño visual: colores, layout, tipografía (JSON)';

INSERT INTO menu_item (codigo, padre_id, orden, etiqueta, label_key, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES
    (
        'ventas-cotizaciones',
        (SELECT id FROM menu_item WHERE codigo = 'ventas'),
        5,
        'Cotizaciones',
        'menu.quotations',
        '/t/:slug/ventas/cotizaciones',
        'file-signature',
        'VENTAS',
        'VENTAS_GESTIONAR',
        'ACTIVO'
    ),
    (
        'ventas-vendedores',
        (SELECT id FROM menu_item WHERE codigo = 'ventas'),
        6,
        'Vendedores',
        'menu.salespeople',
        '/t/:slug/ventas/vendedores',
        'user-check',
        'VENTAS',
        'VENTAS_GESTIONAR',
        'ACTIVO'
    )
ON CONFLICT (codigo) DO UPDATE
SET padre_id = EXCLUDED.padre_id,
    orden = EXCLUDED.orden,
    etiqueta = EXCLUDED.etiqueta,
    label_key = EXCLUDED.label_key,
    ruta_front = EXCLUDED.ruta_front,
    icono = EXCLUDED.icono,
    modulo = EXCLUDED.modulo,
    requiere_permiso_codigo = EXCLUDED.requiere_permiso_codigo,
    estado = EXCLUDED.estado;
