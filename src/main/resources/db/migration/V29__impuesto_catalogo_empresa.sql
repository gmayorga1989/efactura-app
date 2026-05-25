-- Catálogo de impuestos de producto: filas globales (empresa_id NULL) por país + filas propias de la empresa.

ALTER TABLE impuesto_producto_catalogo
    ADD COLUMN IF NOT EXISTS empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE;

ALTER TABLE impuesto_producto_catalogo DROP CONSTRAINT IF EXISTS impuesto_producto_catalogo_pais_iso_tipo_codigo_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_impuesto_prod_cat_global
    ON impuesto_producto_catalogo (pais_iso, tipo, codigo)
    WHERE empresa_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_impuesto_prod_cat_empresa
    ON impuesto_producto_catalogo (empresa_id, tipo, codigo)
    WHERE empresa_id IS NOT NULL;

-- Referencia SRI Ecuador (codigoPorcentaje IVA); ajustables desde la app.
INSERT INTO impuesto_producto_catalogo (id, pais_iso, empresa_id, tipo, codigo, nombre, porcentaje_default, orden, activo)
VALUES
    (gen_random_uuid(), 'EC', NULL, 'IVA', '0', 'IVA 0%', 0, 1, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '2', 'IVA 12%', 12, 2, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '3', 'IVA 14%', 14, 3, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '4', 'IVA 15%', 15, 4, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '5', 'IVA 5%', 5, 5, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '6', 'No objeto de IVA', 0, 6, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IVA', '7', 'Exento de IVA', 0, 7, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'ICE', 'ICE', 'ICE (definir tarifa según normativa)', NULL, 20, TRUE),
    (gen_random_uuid(), 'EC', NULL, 'IRBPNR', 'IRBPNR', 'IRBPNR (definir tarifa según normativa)', NULL, 30, TRUE);
