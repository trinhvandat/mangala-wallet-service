-- V9__create_token_price_history.sql
-- Store historical token prices for 24h change calculation

CREATE TABLE token_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(20) NOT NULL,
    chain_type VARCHAR(20) NOT NULL,
    price_usd DECIMAL(30, 18) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient 24h lookback queries
CREATE INDEX idx_price_history_lookup ON token_price_history(symbol, chain_type, recorded_at DESC);

-- Index for cleanup of old records
CREATE INDEX idx_price_history_cleanup ON token_price_history(recorded_at);

-- Rollback:
-- DROP INDEX idx_price_history_cleanup;
-- DROP INDEX idx_price_history_lookup;
-- DROP TABLE token_price_history;
