-- Accesos laterales para maestros operativos.
-- El backend reemplaza :slug con el tenant actual al entregar el menu al frontend.

INSERT INTO menu_item (codigo, orden, etiqueta, ruta_front, icono, modulo, requiere_permiso_codigo, estado)
VALUES
    (
        'maestros',
        35,
        'Maestros',
        NULL,
        'database',
        'MAESTROS',
        'FACTURA_EMITIR|VENTAS_GESTIONAR|PROVEEDOR_GESTIONAR',
        'ACTIVO'
    )
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
    (
        'maestros-clientes',
        (SELECT id FROM menu_item WHERE codigo = 'maestros'),
        10,
        'Clientes',
        '/t/:slug/clientes',
        'users',
        'MAESTROS',
        'FACTURA_EMITIR|VENTAS_GESTIONAR',
        'ACTIVO'
    ),
    (
        'maestros-proveedores',
        (SELECT id FROM menu_item WHERE codigo = 'maestros'),
        20,
        'Proveedores',
        '/t/:slug/proveedores',
        'truck',
        'MAESTROS',
        'PROVEEDOR_GESTIONAR',
        'ACTIVO'
    ),
    (
        'maestros-productos',
        (SELECT id FROM menu_item WHERE codigo = 'maestros'),
        30,
        'Productos',
        '/t/:slug/productos',
        'package',
        'MAESTROS',
        'FACTURA_EMITIR|VENTAS_GESTIONAR',
        'ACTIVO'
    ),
    (
        'maestros-servicios',
        (SELECT id FROM menu_item WHERE codigo = 'maestros'),
        40,
        'Servicios',
        '/t/:slug/servicios',
        'wrench',
        'MAESTROS',
        'FACTURA_EMITIR|VENTAS_GESTIONAR',
        'ACTIVO'
    )
ON CONFLICT (codigo) DO UPDATE
SET padre_id = EXCLUDED.padre_id,
    orden = EXCLUDED.orden,
    etiqueta = EXCLUDED.etiqueta,
    ruta_front = EXCLUDED.ruta_front,
    icono = EXCLUDED.icono,
    modulo = EXCLUDED.modulo,
    requiere_permiso_codigo = EXCLUDED.requiere_permiso_codigo,
    estado = EXCLUDED.estado;
