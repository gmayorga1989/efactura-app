ALTER TABLE identidad
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS avatar_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ultimo_ping TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_identidad_ultimo_ping ON identidad (ultimo_ping);
