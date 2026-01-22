USE lms_db;

-- Bổ sung 5 settings thiếu trong danh mục SYSTEM
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, data_type, updated_by, updated_at) VALUES
('ALLOW_NEW_REGISTRATIONS', 'true', 'true', 'Cho phép người dùng mới đăng ký tài khoản?', 'SYSTEM', 'BOOLEAN', 'SYSTEM', NOW()),
('PAGE_SIZE_DEFAULT', '10', '10', 'Số dòng mặc định trên mỗi trang (bảng danh sách)', 'SYSTEM', 'NUMBER', 'SYSTEM', NOW()),
('SESSION_TIMEOUT_MINUTES', '30', '30', 'Thời gian timeout phiên đăng nhập (phút)', 'SYSTEM', 'NUMBER', 'SYSTEM', NOW()),
('LIBRARY_NAME', 'Thư viện Trường Học', 'Thư viện Trường Học', 'Tên thư viện hiển thị trên giao diện', 'SYSTEM', 'TEXT', 'SYSTEM', NOW()),
('LIBRARY_RULES', 'Nội quy thư viện:\n1. Giữ im lặng\n2. Không ăn uống\n3. Trả sách đúng hạn', 'Nội quy thư viện:\n1. Giữ im lặng\n2. Không ăn uống\n3. Trả sách đúng hạn', 'Nội quy thư viện (hiển thị trên trang chủ)', 'SYSTEM', 'TEXTAREA', 'SYSTEM', NOW());

-- Verify
SELECT COUNT(*) as total FROM system_settings;
SELECT category, COUNT(*) as count FROM system_settings GROUP BY category ORDER BY category;