-- Descarga SRI: comprobantes recibidos, sync y auditoria
INSERT INTO menu_item (codigo, padre_id, orden, etiqueta, label_key, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES (
    'proveedores-descarga-sri',
    (SELECT id FROM menu_item WHERE codigo = 'proveedores'),
    25,
    'Descarga SRI recibidos',
    'menu.sriDownload',
    '/t/:slug/proveedores/descarga-sri',
    'download',
    'PROVEEDORES',
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
