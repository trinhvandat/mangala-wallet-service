-- V6__add_performance_indexes.sql
-- Additional performance indexes for common query patterns

-- Composite index for wallet listing with pagination (most common query)
CREATE INDEX idx_wallets_user_active_created ON wallets(user_id, is_active, created_at DESC)
    WHERE is_active = true;

-- Composite index for chain-filtered wallet queries
CREATE INDEX idx_wallets_user_chain_active ON wallets(user_id, chain_type, is_active)
    WHERE is_active = true;

-- Index for sync scheduling (find wallets that need balance sync)
CREATE INDEX idx_wallets_sync_schedule ON wallets(last_synced_at)
    WHERE is_active = true;

-- Index for token lookups by coingecko_id (price updates)
CREATE INDEX idx_tokens_coingecko ON tokens(coingecko_id)
    WHERE coingecko_id IS NOT NULL;

-- Index for finding all balances for a user (portfolio view)
-- Uses wallet_id -> user_id join, but helps with balance aggregation
CREATE INDEX idx_wallet_balances_updated ON wallet_balances(updated_at);

-- Index for recent transactions (dashboard view)
-- Note: Using regular index instead of partial index because CURRENT_TIMESTAMP is not IMMUTABLE
CREATE INDEX idx_transactions_recent ON transactions(wallet_id, created_at DESC);

-- Rollback:
-- DROP INDEX idx_transactions_recent;
-- DROP INDEX idx_wallet_balances_updated;
-- DROP INDEX idx_tokens_coingecko;
-- DROP INDEX idx_wallets_sync_schedule;
-- DROP INDEX idx_wallets_user_chain_active;
-- DROP INDEX idx_wallets_user_active_created;
