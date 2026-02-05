-- Migration script for Google OAuth support
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- Copy name to full_name if full_name is null
UPDATE users SET full_name = name WHERE full_name IS NULL AND name IS NOT NULL;
