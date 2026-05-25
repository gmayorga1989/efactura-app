-- El dashboard/inicio lo maneja el shell del frontend como entrada principal.
-- Evita duplicar "Inicio" con la navegacion dinamica jerarquica.
UPDATE menu_item
SET estado = 'INACTIVO'
WHERE codigo = 'inicio';
