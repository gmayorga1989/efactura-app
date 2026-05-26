-- Adjuntos de cotización: archivos subidos además de enlaces externos

ALTER TABLE cotizacion_adjunto
    ALTER COLUMN url DROP NOT NULL;

ALTER TABLE cotizacion_adjunto
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tamano_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS nombre_archivo VARCHAR(255);
