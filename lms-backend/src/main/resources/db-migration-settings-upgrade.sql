-- =====================================================
-- Migration: Upgrade system_settings to Dynamic Configuration Center
-- Date: 2026-01-18
-- Description: Add category, dataType, defaultValue, description, audit fields
-- =====================================================

-- Step 1: Add new columns
ALTER TABLE system_settings
    ADD COLUMN default_value VARCHAR(1000) AFTER setting_value,
    ADD COLUMN description VARCHAR(500) AFTER default_value,
    ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' AFTER description,
    ADD COLUMN data_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' AFTER category,
    ADD COLUMN updated_by VARCHAR(100) AFTER data_type,
    MODIFY COLUMN setting_value VARCHAR(1000) NOT NULL;

-- Step 2: Update existing settings with metadata
UPDATE system_settings SET 
    category = 'LOAN_POLICY',
    data_type = 'NUMBER',
    default_value = '14',
    description = 'Số ngày tối đa được mượn sách (mặc định: 14 ngày)',
    updated_by = 'SYSTEM'
WHERE setting_key = 'LOAN_MAX_DAYS';

UPDATE system_settings SET 
    category = 'LOAN_POLICY',
    data_type = 'NUMBER',
    default_value = '2000',
    description = 'Phí phạt mỗi ngày trễ hạn (đơn vị: VND)',
    updated_by = 'SYSTEM'
WHERE setting_key = 'FINE_PER_DAY';

-- Step 3: Insert new settings for enhanced functionality

-- Loan Policy Settings
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, data_type, updated_by, updated_at)
VALUES 
    ('MAX_BOOKS_PER_USER', '5', '5', 'Số sách tối đa một người được mượn cùng lúc', 'LOAN_POLICY', 'NUMBER', 'SYSTEM', NOW()),
    ('ALLOW_RENEWAL', 'true', 'true', 'Cho phép người dùng gia hạn phiếu mượn?', 'LOAN_POLICY', 'BOOLEAN', 'SYSTEM', NOW()),
    ('MAX_RENEWAL_TIMES', '2', '2', 'Số lần gia hạn tối đa cho mỗi phiếu mượn', 'LOAN_POLICY', 'NUMBER', 'SYSTEM', NOW()),
    ('RESERVATION_DAYS', '3', '3', 'Số ngày giữ chỗ sách (sau đó tự động hủy)', 'LOAN_POLICY', 'NUMBER', 'SYSTEM', NOW())
ON DUPLICATE KEY UPDATE setting_key = setting_key;

-- Email & Notification Settings
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, data_type, updated_by, updated_at)
VALUES 
    ('EMAIL_REMINDER_HOUR', '08:00', '08:00', 'Giờ gửi email nhắc nợ tự động (định dạng HH:mm)', 'EMAIL_NOTIFICATION', 'TIME', 'SYSTEM', NOW()),
    ('EMAIL_ADMIN_ADDRESS', 'admin@library.edu.vn', 'admin@library.edu.vn', 'Email admin nhận phản hồi từ người dùng', 'EMAIL_NOTIFICATION', 'TEXT', 'SYSTEM', NOW()),
    ('EMAIL_OVERDUE_SUBJECT', 'Thông báo trễ hạn - Thư viện', 'Thông báo trễ hạn - Thư viện', 'Tiêu đề email nhắc trễ hạn', 'EMAIL_NOTIFICATION', 'TEXT', 'SYSTEM', NOW()),
    ('EMAIL_OVERDUE_BODY', 'Xin chào,\n\nBạn có sách trễ hạn. Vui lòng trả sách sớm để tránh phí phạt.\n\nTrân trọng,\nThư viện', 
     'Xin chào,\n\nBạn có sách trễ hạn. Vui lòng trả sách sớm để tránh phí phạt.\n\nTrân trọng,\nThư viện',
     'Nội dung email nhắc trễ hạn', 'EMAIL_NOTIFICATION', 'TEXTAREA', 'SYSTEM', NOW()),
    ('ENABLE_EMAIL_NOTIFICATIONS', 'true', 'true', 'Bật/tắt hệ thống gửi email tự động', 'EMAIL_NOTIFICATION', 'BOOLEAN', 'SYSTEM', NOW())
ON DUPLICATE KEY UPDATE setting_key = setting_key;

-- System Settings
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, data_type, updated_by, updated_at)
VALUES 
    ('MAINTENANCE_MODE', 'false', 'false', 'Bật chế độ bảo trì (người dùng không thể đăng nhập)', 'SYSTEM', 'BOOLEAN', 'SYSTEM', NOW()),
    ('ALLOW_NEW_REGISTRATIONS', 'true', 'true', 'Cho phép người dùng mới đăng ký tài khoản?', 'SYSTEM', 'BOOLEAN', 'SYSTEM', NOW()),
    ('PAGE_SIZE_DEFAULT', '10', '10', 'Số dòng mặc định trên mỗi trang (bảng danh sách)', 'SYSTEM', 'NUMBER', 'SYSTEM', NOW()),
    ('SESSION_TIMEOUT_MINUTES', '30', '30', 'Thời gian timeout phiên đăng nhập (phút)', 'SYSTEM', 'NUMBER', 'SYSTEM', NOW()),
    ('LIBRARY_NAME', 'Thư viện Trường Học', 'Thư viện Trường Học', 'Tên thư viện hiển thị trên giao diện', 'SYSTEM', 'TEXT', 'SYSTEM', NOW()),
    ('LIBRARY_RULES', 'Nội quy thư viện:\n1. Giữ im lặng\n2. Không ăn uống\n3. Trả sách đúng hạn', 
     'Nội quy thư viện:\n1. Giữ im lặng\n2. Không ăn uống\n3. Trả sách đúng hạn',
     'Nội quy thư viện (hiển thị trên trang chủ)', 'SYSTEM', 'TEXTAREA', 'SYSTEM', NOW())
ON DUPLICATE KEY UPDATE setting_key = setting_key;

-- Step 4: Create index for faster category queries
CREATE INDEX idx_settings_category ON system_settings(category);

-- =====================================================
-- Verification Queries
-- =====================================================
-- Check all settings grouped by category:
-- SELECT category, COUNT(*) as count FROM system_settings GROUP BY category;

-- View all settings with metadata:
-- SELECT setting_key, setting_value, default_value, category, data_type, updated_by, updated_at 
-- FROM system_settings ORDER BY category, setting_key;
