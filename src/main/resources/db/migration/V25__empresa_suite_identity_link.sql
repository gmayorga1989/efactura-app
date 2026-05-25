-- Vincula empresa eFactura con tenant (company) del Identity Gateway: claim JWT "company_id".
ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS suite_company_id UUID NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_empresa_suite_company_id
    ON empresa (suite_company_id)
    WHERE suite_company_id IS NOT NULL;

COMMENT ON COLUMN empresa.suite_company_id IS 'UUID de auth.company (Suite Identity); debe coincidir con el claim company_id del access token para /auth/suite/exchange';
