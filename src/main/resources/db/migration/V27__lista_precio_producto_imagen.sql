-- Listas de precio por empresa y precios por producto; imagen opcional en producto.

CREATE TABLE lista_precio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    es_default BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_lista_precio_empresa ON lista_precio (empresa_id);

CREATE TABLE producto_precio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES producto (id) ON DELETE CASCADE,
    lista_precio_id UUID NOT NULL REFERENCES lista_precio (id) ON DELETE CASCADE,
    precio NUMERIC(14, 6) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_modificacion TIMESTAMPTZ,
    UNIQUE (producto_id, lista_precio_id)
);

CREATE INDEX idx_producto_precio_producto ON producto_precio (producto_id);

ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS imagen_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(800);

-- Lista BASE por empresa (precio historico en columna producto.precio_unitario).
INSERT INTO lista_precio (id, empresa_id, codigo, nombre, es_default, estado)
SELECT gen_random_uuid(), e.id, 'BASE', 'Lista base', TRUE, 'ACTIVO'
FROM empresa e
WHERE NOT EXISTS (
    SELECT 1 FROM lista_precio lp WHERE lp.empresa_id = e.id AND lp.codigo = 'BASE'
);

INSERT INTO producto_precio (id, producto_id, lista_precio_id, precio, fecha_creacion)
SELECT gen_random_uuid(), p.id, lp.id, p.precio_unitario, NOW()
FROM producto p
JOIN lista_precio lp ON lp.empresa_id = p.empresa_id AND lp.codigo = 'BASE'
WHERE p.precio_unitario IS NOT NULL
  AND p.estado <> 'ELIMINADO'
  AND NOT EXISTS (
    SELECT 1 FROM producto_precio pp WHERE pp.producto_id = p.id AND pp.lista_precio_id = lp.id
  );
