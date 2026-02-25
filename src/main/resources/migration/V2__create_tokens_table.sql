-- V2__create_tokens_table.sql
CREATE TABLE tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chain_type VARCHAR(20) NOT NULL,
    contract_address VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    decimals INT NOT NULL,
    logo_url VARCHAR(500),
    coingecko_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    UNIQUE(chain_type, contract_address)
);

CREATE INDEX idx_tokens_chain_address ON tokens(chain_type, contract_address);
CREATE INDEX idx_tokens_symbol ON tokens(symbol);

-- Rollback:
-- DROP INDEX idx_tokens_symbol;
-- DROP INDEX idx_tokens_chain_address;
-- DROP TABLE tokens;
