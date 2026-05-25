-- Metadata para modelo tenant hibrido.
-- Por ahora todas las empresas operan en la base compartida actual.
-- datasource_key permite agrupar empresas en una misma base dedicada en una fase posterior.

CREATE TABLE tenant_datasource_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    modo_tenant VARCHAR(20) NOT NULL DEFAULT 'SHARED',
    datasource_key VARCHAR(120) NOT NULL DEFAULT 'shared-main',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_creacion VARCHAR(100),
    fecha_modificacion TIMESTAMPTZ,
    usuario_modificacion VARCHAR(100),
    UNIQUE (empresa_id)
);

CREATE INDEX idx_tenant_datasource_key ON tenant_datasource_config (datasource_key, estado);

INSERT INTO tenant_datasource_config (empresa_id, modo_tenant, datasource_key, estado)
SELECT e.id, 'SHARED', 'shared-main', 'ACTIVO'
FROM empresa e
ON CONFLICT (empresa_id) DO NOTHING;
