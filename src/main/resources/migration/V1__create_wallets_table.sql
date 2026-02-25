-- V1__create_wallets_table.sql
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    address VARCHAR(100) NOT NULL,
    chain_type VARCHAR(20) NOT NULL,
    label VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    last_synced_at TIMESTAMP,
    UNIQUE(user_id, address, chain_type)
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_address ON wallets(address);
CREATE INDEX idx_wallets_is_active ON wallets(is_active);

-- Rollback:
-- DROP INDEX idx_wallets_is_active;
-- DROP INDEX idx_wallets_address;
-- DROP INDEX idx_wallets_user_id;
-- DROP TABLE wallets;
