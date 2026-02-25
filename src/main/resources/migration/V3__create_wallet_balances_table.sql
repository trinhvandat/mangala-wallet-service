-- V3__create_wallet_balances_table.sql
-- Stores token balances for wallets, updated by the balance sync scheduler

CREATE TABLE wallet_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    chain_type VARCHAR(20) NOT NULL,
    contract_address VARCHAR(100),  -- NULL for native token
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    decimals INT NOT NULL,
    balance_raw VARCHAR(78) NOT NULL,  -- BigInteger as string (max 2^256)
    balance DECIMAL(38, 18) NOT NULL,  -- Human-readable balance
    last_synced_at TIMESTAMP NOT NULL DEFAULT now(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    UNIQUE(wallet_id, chain_type, contract_address)
);

-- Index for efficient balance lookups by wallet
CREATE INDEX idx_wallet_balances_wallet_id ON wallet_balances(wallet_id);

-- Index for finding all balances of a specific token across wallets
CREATE INDEX idx_wallet_balances_contract ON wallet_balances(chain_type, contract_address);

-- Index for sync scheduling (find stale balances)
CREATE INDEX idx_wallet_balances_synced_at ON wallet_balances(last_synced_at);

-- Partial index for non-zero balances (common query pattern)
CREATE INDEX idx_wallet_balances_nonzero ON wallet_balances(wallet_id)
    WHERE balance > 0;

-- Rollback:
-- DROP INDEX idx_wallet_balances_nonzero;
-- DROP INDEX idx_wallet_balances_synced_at;
-- DROP INDEX idx_wallet_balances_contract;
-- DROP INDEX idx_wallet_balances_wallet_id;
-- DROP TABLE wallet_balances;
