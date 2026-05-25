-- Identidad de login única + membresías por empresa.
-- Backfill: una fila identidad por email (lower(trim)); una membresía por antigua fila usuario.

CREATE TABLE identidad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    mfa_secret VARCHAR(100),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login TIMESTAMPTZ,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE TABLE _migr_usuario_identidad (
    usuario_id UUID PRIMARY KEY,
    identidad_id UUID NOT NULL REFERENCES identidad (id) ON DELETE CASCADE
);

INSERT INTO
    identidad (
        id,
        email,
        password_hash,
        nombre,
        mfa_secret,
        mfa_habilitado,
        ultimo_login,
        intentos_fallidos,
        bloqueado_hasta,
        estado,
        fecha_creacion,
        usuario_creacion,
        fecha_modificacion,
        usuario_modificacion
    )
SELECT DISTINCT
    ON (lower(trim(u.email))) gen_random_uuid(),
    lower(trim(u.email)),
    u.password_hash,
    u.nombre,
    u.mfa_secret,
    u.mfa_habilitado,
    u.ultimo_login,
    u.intentos_fallidos,
    u.bloqueado_hasta,
    u.estado,
    u.fecha_creacion,
    u.usuario_creacion,
    u.fecha_modificacion,
    u.usuario_modificacion
FROM
    usuario u
ORDER BY
    lower(trim(u.email)),
    u.fecha_creacion;

INSERT INTO
    _migr_usuario_identidad (usuario_id, identidad_id)
SELECT
    u.id,
    i.id
FROM
    usuario u
    JOIN identidad i ON i.email = lower(trim(u.email));

CREATE TABLE membresia_empresa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identidad_id UUID NOT NULL REFERENCES identidad (id) ON DELETE CASCADE,
    empresa_id UUID REFERENCES empresa (id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_invitacion TIMESTAMPTZ,
    fecha_aceptacion TIMESTAMPTZ,
    es_ultima_empresa_usada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100)
);

CREATE UNIQUE INDEX idx_membresia_identidad_empresa_nonempty ON membresia_empresa (identidad_id, empresa_id)
WHERE
    empresa_id IS NOT NULL;

CREATE UNIQUE INDEX idx_membresia_identidad_plataforma ON membresia_empresa (identidad_id)
WHERE
    empresa_id IS NULL;

CREATE TABLE membresia_rol (
    membresia_id UUID NOT NULL REFERENCES membresia_empresa (id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    PRIMARY KEY (membresia_id, rol_id)
);

INSERT INTO
    membresia_empresa (
        id,
        identidad_id,
        empresa_id,
        estado,
        fecha_creacion,
        usuario_creacion,
        fecha_modificacion,
        usuario_modificacion
    )
SELECT
    gen_random_uuid(),
    m.identidad_id,
    u.empresa_id,
    CASE
        WHEN u.estado = 'ACTIVO' THEN 'ACTIVO'
        ELSE u.estado
    END,
    u.fecha_creacion,
    u.usuario_creacion,
    u.fecha_modificacion,
    u.usuario_modificacion
FROM
    usuario u
    JOIN _migr_usuario_identidad m ON m.usuario_id = u.id;

INSERT INTO
    membresia_rol (membresia_id, rol_id)
SELECT
    me.id,
    ur.rol_id
FROM
    usuario_rol ur
    JOIN usuario u ON u.id = ur.usuario_id
    JOIN _migr_usuario_identidad map ON map.usuario_id = u.id
    JOIN membresia_empresa me ON me.identidad_id = map.identidad_id
    AND (
        me.empresa_id IS NOT DISTINCT
        FROM
            u.empresa_id
    );

ALTER TABLE refresh_token
ADD COLUMN identidad_id UUID REFERENCES identidad (id);

ALTER TABLE refresh_token
ADD COLUMN empresa_id UUID REFERENCES empresa (id);

UPDATE refresh_token rt
SET
    identidad_id = map.identidad_id,
    empresa_id = u.empresa_id
FROM
    usuario u
    JOIN _migr_usuario_identidad map ON map.usuario_id = u.id
WHERE
    rt.usuario_id = u.id;

ALTER TABLE refresh_token
ALTER COLUMN identidad_id
SET NOT NULL;

ALTER TABLE refresh_token DROP CONSTRAINT refresh_token_usuario_id_fkey;

ALTER TABLE refresh_token DROP COLUMN usuario_id;

DROP TABLE usuario_rol;

DROP TABLE usuario;

DROP TABLE _migr_usuario_identidad;

ALTER TABLE empresa
ADD COLUMN slug VARCHAR(100);

CREATE UNIQUE INDEX idx_empresa_slug_lower ON empresa (lower(slug))
WHERE
    slug IS NOT NULL;
