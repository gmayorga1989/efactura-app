-- Tarifas ICE de referencia para Ecuador (SRI). Sustituye el ítem genérico único por opciones con %.

UPDATE impuesto_producto_catalogo
SET activo = FALSE
WHERE pais_iso = 'EC'
  AND empresa_id IS NULL
  AND tipo = 'ICE'
  AND codigo = 'ICE';

INSERT INTO impuesto_producto_catalogo (id, pais_iso, empresa_id, tipo, codigo, nombre, porcentaje_default, orden, activo)
SELECT gen_random_uuid(), 'EC', NULL, 'ICE', v.codigo, v.nombre, v.porcentaje, v.orden, TRUE
FROM (
    VALUES
        ('0', 'Sin ICE (0%)', 0::numeric, 21),
        ('3025', 'Bebidas alcohólicas — tarifa ref. 75%', 75::numeric, 22),
        ('3023', 'Cerveza — tarifa ref. 75%', 75::numeric, 23),
        ('3041', 'Bebidas gaseosas con azúcar — tarifa ref. 35%', 35::numeric, 24),
        ('3031', 'Cigarrillos — tarifa ref. 150%', 150::numeric, 25),
        ('3073', 'Vehículos (segmento ref.) — tarifa ref. 5%', 5::numeric, 26),
        ('3080', 'Perfumes y aguas de tocador — tarifa ref. 20%', 20::numeric, 27),
        ('3092', 'Armas de fuego, armas deportivas y municiones — tarifa ref. 300%', 300::numeric, 28),
        ('3610', 'Servicios de telefonía móvil — tarifa ref. 15%', 15::numeric, 29)
) AS v(codigo, nombre, porcentaje, orden)
WHERE NOT EXISTS (
    SELECT 1
    FROM impuesto_producto_catalogo c
    WHERE c.pais_iso = 'EC'
      AND c.empresa_id IS NULL
      AND c.tipo = 'ICE'
      AND c.codigo = v.codigo
);
