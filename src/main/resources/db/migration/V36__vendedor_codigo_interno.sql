-- Código interno automático por empresa; codigo queda como referencia adicional opcional.

ALTER TABLE vendedor ADD COLUMN IF NOT EXISTS codigo_interno VARCHAR(30);

WITH numerados AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY empresa_id ORDER BY fecha_creacion, id) AS rn
    FROM vendedor
    WHERE codigo_interno IS NULL
)
UPDATE vendedor v
SET codigo_interno = 'VEN-' || LPAD(n.rn::text, 5, '0')
FROM numerados n
WHERE v.id = n.id;

ALTER TABLE vendedor ALTER COLUMN codigo_interno SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vendedor_empresa_codigo_interno
    ON vendedor (empresa_id, codigo_interno);

COMMENT ON COLUMN vendedor.codigo_interno IS 'Identificador automático del vendedor (VEN-00001)';
COMMENT ON COLUMN vendedor.codigo IS 'Código adicional opcional definido por el usuario';
