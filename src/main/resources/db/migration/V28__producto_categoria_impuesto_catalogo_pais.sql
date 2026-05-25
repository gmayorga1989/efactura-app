-- País de la empresa (catálogos tributarios); categorías de producto multinivel; catálogo de impuestos/cargos adicionales por país.

ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS pais_iso VARCHAR(2) NOT NULL DEFAULT 'EC';

CREATE TABLE producto_categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    parent_id UUID REFERENCES producto_categoria (id) ON DELETE SET NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_producto_categoria_empresa ON producto_categoria (empresa_id);
CREATE INDEX idx_producto_categoria_parent ON producto_categoria (parent_id);

CREATE TABLE impuesto_producto_catalogo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pais_iso VARCHAR(2) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    porcentaje_default NUMERIC(10, 6),
    orden INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (pais_iso, tipo, codigo)
);

INSERT INTO impuesto_producto_catalogo (id, pais_iso, tipo, codigo, nombre, porcentaje_default, orden, activo)
VALUES
    (gen_random_uuid(), 'EC', 'OTRO', 'IBC', 'Impuesto Bolsa de Valores (referencia)', NULL, 10, TRUE),
    (gen_random_uuid(), 'EC', 'OTRO', 'MUNICIPAL', 'Tasa o contribucion municipal (referencia)', NULL, 20, TRUE),
    (gen_random_uuid(), 'EC', 'OTRO', 'ISLR_RET', 'Retencion ISLR aplicable (referencia)', NULL, 30, TRUE),
    (gen_random_uuid(), 'EC', 'OTRO', 'OTRO_MANUAL', 'Otro cargo / impuesto (definir detalle en observaciones)', NULL, 90, TRUE);

ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS categoria_id UUID REFERENCES producto_categoria (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_producto_categoria_fk ON producto (categoria_id);
