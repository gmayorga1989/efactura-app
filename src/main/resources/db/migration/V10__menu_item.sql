-- Menú dinámico (filtrado por permisos en servicio)

CREATE TABLE menu_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(80) NOT NULL UNIQUE,
    padre_id UUID REFERENCES menu_item (id) ON DELETE SET NULL,
    orden INT NOT NULL DEFAULT 0,
    etiqueta VARCHAR(150) NOT NULL,
    ruta_front VARCHAR(300),
    icono VARCHAR(80),
    modulo VARCHAR(50),
    requiere_permiso_codigo VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_item_estado_orden ON menu_item (estado, orden);

INSERT INTO
    menu_item (codigo, orden, etiqueta, ruta_front, modulo, requiere_permiso_codigo)
VALUES ('inicio', 10, 'Inicio', '/t/:slug/inicio', 'CORE', NULL),
    (
        'facturas',
        20,
        'Facturas',
        '/t/:slug/facturas',
        'EMISION',
        'FACTURA_EMITIR'
    ),
    (
        'reportes',
        30,
        'Reportes',
        '/t/:slug/reportes',
        'REPORTES',
        'REPORTE_VER'
    ),
    (
        'config-empresa',
        40,
        'Empresa',
        '/t/:slug/configuracion',
        'CORE',
        'EMPRESA_ADMIN'
    );
