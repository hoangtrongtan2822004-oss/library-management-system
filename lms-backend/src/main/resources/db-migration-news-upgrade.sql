-- News table schema upgrade
-- Add new columns for rich content, pinning, scheduling, and status

ALTER TABLE news
ADD COLUMN cover_image_url VARCHAR(500),
ADD COLUMN is_pinned BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN published_at TIMESTAMP,
ADD COLUMN status VARCHAR(20) DEFAULT 'PUBLISHED' NOT NULL;

-- Create index for faster queries
CREATE INDEX idx_news_pinned_created ON news(is_pinned DESC, created_at DESC);
CREATE INDEX idx_news_status_published ON news(status, published_at);

-- Update existing records to PUBLISHED status with current timestamp as published_at
UPDATE news 
SET published_at = created_at, 
    status = 'PUBLISHED', 
    is_pinned = FALSE
WHERE published_at IS NULL;
