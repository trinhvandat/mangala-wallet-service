-- V5__create_transactions_table.sql
-- Tracks blockchain transactions for wallets

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    chain_type VARCHAR(20) NOT NULL,
    tx_hash VARCHAR(100) NOT NULL,
    block_number BIGINT,
    block_timestamp TIMESTAMP,
    from_address VARCHAR(100) NOT NULL,
    to_address VARCHAR(100),  -- NULL for contract creation
    value_raw VARCHAR(78) NOT NULL,  -- BigInteger as string
    value DECIMAL(38, 18) NOT NULL,  -- Human-readable value
    contract_address VARCHAR(100),  -- NULL for native token transfer
    token_symbol VARCHAR(20),
    token_decimals INT,
    gas_used BIGINT,
    gas_price VARCHAR(78),  -- In wei
    tx_fee DECIMAL(38, 18),
    tx_type VARCHAR(30) NOT NULL,  -- 'TRANSFER_IN', 'TRANSFER_OUT', 'SWAP', 'APPROVE', 'CONTRACT_CALL'
    status VARCHAR(20) NOT NULL,  -- 'PENDING', 'CONFIRMED', 'FAILED'
    confirmations INT DEFAULT 0,
    raw_data JSONB,  -- Full transaction data from chain
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    UNIQUE(chain_type, tx_hash, wallet_id)
);

-- Index for finding transactions by wallet
CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);

-- Index for finding transactions by hash (common lookup)
CREATE INDEX idx_transactions_tx_hash ON transactions(tx_hash);

-- Index for finding transactions by wallet and time (history queries)
CREATE INDEX idx_transactions_wallet_time ON transactions(wallet_id, block_timestamp DESC);

-- Index for pending transactions that need confirmation checking
CREATE INDEX idx_transactions_pending ON transactions(chain_type, status)
    WHERE status = 'PENDING';

-- Index for finding transactions to/from specific address
CREATE INDEX idx_transactions_addresses ON transactions(from_address, to_address);

-- Index for token-specific transaction queries
CREATE INDEX idx_transactions_token ON transactions(chain_type, contract_address)
    WHERE contract_address IS NOT NULL;

-- Rollback:
-- DROP INDEX idx_transactions_token;
-- DROP INDEX idx_transactions_addresses;
-- DROP INDEX idx_transactions_pending;
-- DROP INDEX idx_transactions_wallet_time;
-- DROP INDEX idx_transactions_tx_hash;
-- DROP INDEX idx_transactions_wallet_id;
-- DROP TABLE transactions;
