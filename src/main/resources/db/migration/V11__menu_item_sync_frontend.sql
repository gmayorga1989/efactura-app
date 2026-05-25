-- Sincronizar menú con rutas reales del frontend Angular

-- Inicio: ruta canónica `dashboard` (antes `/inicio` redirigía solo en el front)
UPDATE menu_item
SET
    ruta_front = '/t/:slug/dashboard'
WHERE
    codigo = 'inicio';

-- API keys (misma política de visibilidad que gestión de empresa en el panel)
INSERT INTO
    menu_item (codigo, orden, etiqueta, ruta_front, modulo, requiere_permiso_codigo)
VALUES (
        'integraciones-api-keys',
        25,
        'API keys',
        '/t/:slug/integraciones/api-keys',
        'CORE',
        'EMPRESA_ADMIN')
ON CONFLICT (codigo) DO
UPDATE
SET
    orden = EXCLUDED.orden,
    etiqueta = EXCLUDED.etiqueta,
    ruta_front = EXCLUDED.ruta_front,
    modulo = EXCLUDED.modulo,
    requiere_permiso_codigo = EXCLUDED.requiere_permiso_codigo;
