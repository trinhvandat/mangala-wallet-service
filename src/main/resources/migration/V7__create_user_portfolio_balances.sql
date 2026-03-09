-- V7__create_user_portfolio_balances.sql
-- Aggregated portfolio balances per user per token

CREATE TABLE user_portfolio_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    chain_type VARCHAR(20) NOT NULL,
    contract_address VARCHAR(100),  -- NULL for native token
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    total_balance DECIMAL(38, 18) NOT NULL DEFAULT 0,
    wallet_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    last_updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, chain_type, contract_address)
);

-- Index for user portfolio queries
CREATE INDEX idx_portfolio_balances_user ON user_portfolio_balances(user_id);

-- Index for finding recently updated portfolios
CREATE INDEX idx_portfolio_balances_updated ON user_portfolio_balances(last_updated_at);

-- Index for chain-specific queries
CREATE INDEX idx_portfolio_balances_chain ON user_portfolio_balances(user_id, chain_type);

-- Partial index for non-zero balances (common query pattern)
CREATE INDEX idx_portfolio_balances_nonzero ON user_portfolio_balances(user_id)
    WHERE total_balance > 0;

-- Rollback:
-- DROP INDEX idx_portfolio_balances_nonzero;
-- DROP INDEX idx_portfolio_balances_chain;
-- DROP INDEX idx_portfolio_balances_updated;
-- DROP INDEX idx_portfolio_balances_user;
-- DROP TABLE user_portfolio_balances;
