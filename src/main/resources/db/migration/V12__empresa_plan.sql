-- Plan comercial por empresa (límites y código de plan para UI "Mi plan")
ALTER TABLE empresa
    ADD COLUMN plan_codigo VARCHAR(64) NOT NULL DEFAULT 'DEMO';

ALTER TABLE empresa
    ADD COLUMN plan_limite_mes INTEGER NULL;

COMMENT ON COLUMN empresa.plan_limite_mes IS 'Máximo de comprobantes emitidos por mes civil; NULL = sin límite.';

UPDATE empresa
SET plan_codigo = 'DEMO',
    plan_limite_mes = 5000
WHERE ruc = '1790012345001';
