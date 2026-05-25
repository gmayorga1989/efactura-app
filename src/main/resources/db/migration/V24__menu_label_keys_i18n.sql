ALTER TABLE menu_item
    ADD COLUMN IF NOT EXISTS label_key VARCHAR(120);

UPDATE menu_item SET label_key = 'menu.dashboard' WHERE codigo = 'inicio';
UPDATE menu_item SET label_key = 'menu.admin' WHERE codigo = 'admin';
UPDATE menu_item SET label_key = 'menu.sales' WHERE codigo = 'ventas';
UPDATE menu_item SET label_key = 'menu.suppliers' WHERE codigo = 'proveedores';
UPDATE menu_item SET label_key = 'menu.reports' WHERE codigo IN ('reportes-grupo', 'reportes');
UPDATE menu_item SET label_key = 'menu.invoices' WHERE codigo IN ('facturas');
UPDATE menu_item SET label_key = 'menu.creditNotes' WHERE codigo = 'ventas-notas-credito';
UPDATE menu_item SET label_key = 'menu.debitNotes' WHERE codigo = 'ventas-notas-debito';
UPDATE menu_item SET label_key = 'menu.guides' WHERE codigo = 'ventas-guias';
UPDATE menu_item SET label_key = 'menu.withholdings' WHERE codigo = 'proveedores-retenciones';
UPDATE menu_item SET label_key = 'menu.purchaseSettlements' WHERE codigo = 'proveedores-liquidaciones';
UPDATE menu_item SET label_key = 'menu.company' WHERE codigo IN ('config-empresa', 'admin-empresa');
UPDATE menu_item SET label_key = 'menu.branches' WHERE codigo = 'admin-sucursales';
UPDATE menu_item SET label_key = 'menu.users' WHERE codigo = 'admin-usuarios';
UPDATE menu_item SET label_key = 'menu.invitations' WHERE codigo = 'admin-invitaciones';
UPDATE menu_item SET label_key = 'menu.roles' WHERE codigo = 'admin-roles';
UPDATE menu_item SET label_key = 'menu.apiKeys' WHERE codigo = 'integraciones-api-keys';
UPDATE menu_item SET label_key = 'menu.masters' WHERE codigo = 'maestros';
UPDATE menu_item SET label_key = 'menu.customers' WHERE codigo = 'maestros-clientes';
UPDATE menu_item SET label_key = 'menu.providers' WHERE codigo = 'maestros-proveedores';
UPDATE menu_item SET label_key = 'menu.products' WHERE codigo = 'maestros-productos';
UPDATE menu_item SET label_key = 'menu.services' WHERE codigo = 'maestros-servicios';
