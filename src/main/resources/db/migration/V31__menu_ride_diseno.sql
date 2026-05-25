-- Diseño RIDE: pantalla de personalización de representación impresa
INSERT INTO menu_item (codigo, padre_id, orden, etiqueta, label_key, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES (
    'admin-ride-diseno',
    (SELECT id FROM menu_item WHERE codigo = 'admin'),
    15,
    'Diseno RIDE',
    'menu.rideDesign',
    '/t/:slug/admin/ride-diseno',
    'palette',
    'ADMIN',
    'EMPRESA_ADMIN',
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
