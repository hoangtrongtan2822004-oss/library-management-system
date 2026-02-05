-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_id INT,
    related_type VARCHAR(50),
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample notifications for testing (optional)
-- INSERT INTO notifications (user_id, type, title, message, is_read) 
-- VALUES 
-- (2, 'LOAN_DUE', 'Sách sắp đến hạn trả', 'Sách "Harry Potter" sẽ đến hạn trả vào 05/02/2026', FALSE),
-- (2, 'LOAN_OVERDUE', 'Sách quá hạn', 'Sách "Đắc Nhân Tâm" đã quá hạn 2 ngày', FALSE);
