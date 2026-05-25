-- Invitaciones a empresa (pendientes hasta aceptación con token).

CREATE TABLE membresia_invitacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invitado_por_email VARCHAR(255)
);

CREATE UNIQUE INDEX idx_membresia_inv_pendiente_empresa_email ON membresia_invitacion (empresa_id, lower(email))
WHERE
    estado = 'PENDIENTE';

CREATE INDEX idx_membresia_inv_token_hash ON membresia_invitacion (token_hash)
WHERE
    estado = 'PENDIENTE';
