INSERT INTO permiso (codigo, descripcion, modulo)
SELECT 'PLATFORM_ADMIN',
    'Administración de la plataforma SaaS',
    'PLATFORM'
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            permiso
        WHERE
            codigo = 'PLATFORM_ADMIN');
