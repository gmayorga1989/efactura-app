-- Permisos para abrir otras aplicaciones de la suite desde eFactura (asignables por rol).

INSERT INTO permiso (codigo, descripcion, modulo)
VALUES
    ('SUITE_APP_CARTERA', 'Abrir Suite Cartera (enlace via shell / SSO)', 'SUITE'),
    ('SUITE_APP_POS', 'Abrir Suite POS (enlace via shell)', 'SUITE')
ON CONFLICT (codigo) DO UPDATE
SET descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    estado = 'ACTIVO';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('SUITE_APP_CARTERA', 'SUITE_APP_POS')
WHERE r.codigo = 'PLATFORM_ADMIN'
  AND r.empresa_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('SUITE_APP_CARTERA', 'SUITE_APP_POS')
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;
