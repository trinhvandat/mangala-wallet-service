-- V8__add_deleted_at_to_wallets.sql
-- Add deleted_at timestamp for soft delete audit trail

ALTER TABLE wallets ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_wallets_deleted_at ON wallets(deleted_at);

-- Rollback:
-- DROP INDEX idx_wallets_deleted_at;
-- ALTER TABLE wallets DROP COLUMN deleted_at;
