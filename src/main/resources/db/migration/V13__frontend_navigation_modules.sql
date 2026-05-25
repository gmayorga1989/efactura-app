-- Navegacion jerarquica y permisos de modulos funcionales del frontend

INSERT INTO permiso (codigo, descripcion, modulo)
VALUES
    ('COMPROBANTE_MONITOR', 'Consultar monitor de comprobantes electronicos', 'COMPROBANTES'),
    ('PROVEEDOR_GESTIONAR', 'Gestionar retenciones y liquidaciones de proveedores', 'PROVEEDORES'),
    ('VENTAS_GESTIONAR', 'Gestionar notas de credito, notas de debito y guias', 'VENTAS')
ON CONFLICT (codigo) DO UPDATE
SET descripcion = EXCLUDED.descripcion,
    modulo = EXCLUDED.modulo,
    estado = 'ACTIVO';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'EMPRESA_ADMIN',
    'FACTURA_EMITIR',
    'COMPROBANTE_MONITOR',
    'REPORTE_VER',
    'PROVEEDOR_GESTIONAR',
    'VENTAS_GESTIONAR'
)
WHERE r.codigo IN ('ADMIN', 'PLATFORM_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO menu_item (codigo, orden, etiqueta, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES
    ('admin', 10, 'Administracion', NULL, 'settings', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('ventas', 20, 'Ventas', NULL, 'receipt', 'VENTAS', NULL, 'ACTIVO'),
    ('proveedores', 30, 'Proveedores', NULL, 'truck', 'PROVEEDORES', 'PROVEEDOR_GESTIONAR', 'ACTIVO'),
    ('reportes-grupo', 40, 'Reportes', NULL, 'bar-chart-3', 'REPORTES', 'REPORTE_VER', 'ACTIVO'),
    ('comprobantes-electronicos', 50, 'Comprobantes electronicos', '/t/:slug/comprobantes-electronicos', 'activity', 'COMPROBANTES', 'COMPROBANTE_MONITOR', 'ACTIVO')
ON CONFLICT (codigo) DO UPDATE
SET orden = EXCLUDED.orden,
    etiqueta = EXCLUDED.etiqueta,
    ruta_front = EXCLUDED.ruta_front,
    icono = EXCLUDED.icono,
    modulo = EXCLUDED.modulo,
    requiere_permiso_codigo = EXCLUDED.requiere_permiso_codigo,
    estado = EXCLUDED.estado,
    padre_id = NULL;

INSERT INTO menu_item (codigo, padre_id, orden, etiqueta, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES
    ('admin-empresa', (SELECT id FROM menu_item WHERE codigo = 'admin'), 10, 'Empresa', '/t/:slug/admin/empresa', 'building-2', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('admin-sucursales', (SELECT id FROM menu_item WHERE codigo = 'admin'), 20, 'Sucursal y emision', '/t/:slug/admin/sucursales', 'store', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('admin-usuarios', (SELECT id FROM menu_item WHERE codigo = 'admin'), 30, 'Usuarios', '/t/:slug/admin/usuarios', 'users', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('admin-invitaciones', (SELECT id FROM menu_item WHERE codigo = 'admin'), 40, 'Invitaciones', '/t/:slug/admin/invitaciones', 'mail-plus', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('admin-roles', (SELECT id FROM menu_item WHERE codigo = 'admin'), 50, 'Roles', '/t/:slug/admin/roles', 'shield-check', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('integraciones-api-keys', (SELECT id FROM menu_item WHERE codigo = 'admin'), 60, 'API keys', '/t/:slug/integraciones/api-keys', 'key-round', 'ADMIN', 'EMPRESA_ADMIN', 'ACTIVO'),
    ('facturas', (SELECT id FROM menu_item WHERE codigo = 'ventas'), 10, 'Factura', '/t/:slug/facturas', 'file-text', 'VENTAS', 'FACTURA_EMITIR', 'ACTIVO'),
    ('ventas-notas-credito', (SELECT id FROM menu_item WHERE codigo = 'ventas'), 20, 'Notas credito', '/t/:slug/ventas/notas-credito', 'undo-2', 'VENTAS', 'VENTAS_GESTIONAR', 'ACTIVO'),
    ('ventas-notas-debito', (SELECT id FROM menu_item WHERE codigo = 'ventas'), 30, 'Notas debito', '/t/:slug/ventas/notas-debito', 'redo-2', 'VENTAS', 'VENTAS_GESTIONAR', 'ACTIVO'),
    ('ventas-guias', (SELECT id FROM menu_item WHERE codigo = 'ventas'), 40, 'Guias', '/t/:slug/ventas/guias', 'route', 'VENTAS', 'VENTAS_GESTIONAR', 'ACTIVO'),
    ('proveedores-retenciones', (SELECT id FROM menu_item WHERE codigo = 'proveedores'), 10, 'Retenciones', '/t/:slug/proveedores/retenciones', 'badge-percent', 'PROVEEDORES', 'PROVEEDOR_GESTIONAR', 'ACTIVO'),
    ('proveedores-liquidaciones', (SELECT id FROM menu_item WHERE codigo = 'proveedores'), 20, 'Liquidaciones', '/t/:slug/proveedores/liquidaciones', 'shopping-bag', 'PROVEEDORES', 'PROVEEDOR_GESTIONAR', 'ACTIVO'),
    ('reportes', (SELECT id FROM menu_item WHERE codigo = 'reportes-grupo'), 10, 'Reportes', '/t/:slug/reportes', 'bar-chart-3', 'REPORTES', 'REPORTE_VER', 'ACTIVO')
ON CONFLICT (codigo) DO UPDATE
SET padre_id = EXCLUDED.padre_id,
    orden = EXCLUDED.orden,
    etiqueta = EXCLUDED.etiqueta,
    ruta_front = EXCLUDED.ruta_front,
    icono = EXCLUDED.icono,
    modulo = EXCLUDED.modulo,
    requiere_permiso_codigo = EXCLUDED.requiere_permiso_codigo,
    estado = EXCLUDED.estado;

UPDATE menu_item
SET estado = 'INACTIVO'
WHERE codigo IN ('config-empresa');
