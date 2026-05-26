-- Item de menú: Diseño de cotización (plantilla empresa)

INSERT INTO menu_item (codigo, padre_id, orden, etiqueta, label_key, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES
    (
        'ventas-cotizaciones-diseno',
        (SELECT id FROM menu_item WHERE codigo = 'ventas'),
        6,
        'Diseño cotización',
        'menu.quotationDesign',
        '/t/:slug/ventas/cotizaciones/diseno',
        'palette',
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

UPDATE menu_item
SET orden = 7
WHERE codigo = 'ventas-vendedores'
  AND orden = 6;

