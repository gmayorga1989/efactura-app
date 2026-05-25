ALTER TABLE consumo_comprobante
    ADD CONSTRAINT fk_consumo_comprobante
        FOREIGN KEY (comprobante_id) REFERENCES comprobante (id) ON DELETE CASCADE;
