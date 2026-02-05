-- Migration script for Facebook OAuth support
ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(255) NULL UNIQUE;

CREATE INDEX IF NOT EXISTS idx_users_facebook_id ON users(facebook_id);
