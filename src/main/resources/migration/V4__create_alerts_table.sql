-- V4__create_alerts_table.sql
-- Price alerts for wallet tokens

CREATE TABLE price_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    wallet_id UUID REFERENCES wallets(id) ON DELETE SET NULL,
    chain_type VARCHAR(20) NOT NULL,
    contract_address VARCHAR(100),  -- NULL for native token
    symbol VARCHAR(20) NOT NULL,
    alert_type VARCHAR(20) NOT NULL,  -- 'PRICE_ABOVE', 'PRICE_BELOW', 'PERCENT_CHANGE'
    target_price DECIMAL(38, 18),  -- Target price for PRICE_ABOVE/PRICE_BELOW
    target_percent DECIMAL(10, 4),  -- Target percentage for PERCENT_CHANGE
    reference_price DECIMAL(38, 18),  -- Reference price for percentage calculations
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_triggered BOOLEAN NOT NULL DEFAULT false,
    triggered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    expires_at TIMESTAMP  -- Optional expiration
);

-- Index for finding user's alerts
CREATE INDEX idx_price_alerts_user_id ON price_alerts(user_id);

-- Index for active alerts that need checking
CREATE INDEX idx_price_alerts_active ON price_alerts(chain_type, contract_address, is_active)
    WHERE is_active = true AND is_triggered = false;

-- Index for wallet-specific alerts
CREATE INDEX idx_price_alerts_wallet_id ON price_alerts(wallet_id)
    WHERE wallet_id IS NOT NULL;

-- Index for cleanup of expired alerts
CREATE INDEX idx_price_alerts_expires ON price_alerts(expires_at)
    WHERE expires_at IS NOT NULL AND is_active = true;

-- Rollback:
-- DROP INDEX idx_price_alerts_expires;
-- DROP INDEX idx_price_alerts_wallet_id;
-- DROP INDEX idx_price_alerts_active;
-- DROP INDEX idx_price_alerts_user_id;
-- DROP TABLE price_alerts;
