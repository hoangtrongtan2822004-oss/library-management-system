-- ============================================
-- Feature 9: Gamification Upgrade - New Tables
-- ============================================

-- Table: rewards (Phần thưởng có thể đổi điểm)
CREATE TABLE IF NOT EXISTS rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(10),
    cost INT NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT 'extension, priority, cosmetic, special',
    available BOOLEAN NOT NULL DEFAULT TRUE,
    max_redemptions INT NULL COMMENT 'Null = unlimited',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- Table: user_rewards (Lịch sử đổi thưởng của user)
CREATE TABLE IF NOT EXISTS user_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    reward_id BIGINT NOT NULL,
    points_spent INT NOT NULL,
    redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reward_id) REFERENCES rewards(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_reward_id (reward_id)
);

-- Table: daily_quests (Nhiệm vụ hàng ngày)
CREATE TABLE IF NOT EXISTS daily_quests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    points INT NOT NULL,
    quest_type VARCHAR(50) NOT NULL COMMENT 'login, search, review, borrow, return',
    target INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_quest_type (quest_type)
);

-- Table: user_quest_progress (Tiến độ nhiệm vụ của user)
CREATE TABLE IF NOT EXISTS user_quest_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    quest_id BIGINT NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    date DATE NOT NULL,
    points_earned INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES daily_quests(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_quest_date (user_id, quest_id, date),
    INDEX idx_user_date (user_id, date)
);

-- Table: point_transactions (Lịch sử giao dịch điểm)
CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    points INT NOT NULL COMMENT 'Positive = earned, Negative = spent',
    transaction_type VARCHAR(50) NOT NULL COMMENT 'borrow, return, review, reward, admin, quest',
    reason VARCHAR(500),
    reference_id BIGINT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    balance_after INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_timestamp (user_id, timestamp DESC)
);

-- Add streak_freeze_count to user_points
ALTER TABLE user_points 
ADD COLUMN IF NOT EXISTS streak_freeze_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần freeze streak đã mua';

-- ============================================
-- Insert default rewards
-- ============================================
INSERT INTO rewards (name, description, icon, cost, category, available, max_redemptions) VALUES
('Vé Gia Hạn 7 Ngày', 'Gia hạn thêm 7 ngày cho sách đang mượn', '🎫', 500, 'extension', TRUE, NULL),
('Ưu Tiên Mượn Sách', 'Được ưu tiên khi sách hot có người trả', '⭐', 1000, 'priority', TRUE, NULL),
('Avatar Viền Vàng', 'Trang trí avatar với viền vàng sang chảnh', '🖼️', 800, 'cosmetic', TRUE, NULL),
('Streak Freeze', 'Bảo vệ chuỗi hoạt động khi bỏ lỡ 1 ngày', '❄️', 200, 'special', TRUE, NULL);

-- ============================================
-- Insert default daily quests
-- ============================================
INSERT INTO daily_quests (title, description, points, quest_type, target, active) VALUES
('Đăng Nhập Hàng Ngày', 'Đăng nhập vào hệ thống', 10, 'login', 1, TRUE),
('Tìm Kiếm Sách', 'Tìm kiếm sách trong thư viện', 5, 'search', 1, TRUE),
('Viết Đánh Giá', 'Viết đánh giá cho 1 cuốn sách', 20, 'review', 1, TRUE);
