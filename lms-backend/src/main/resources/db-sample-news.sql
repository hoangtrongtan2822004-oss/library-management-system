-- Sample News Data for Library Management System
-- Run this script to populate the news table with sample data

INSERT INTO news (title, content, created_at, updated_at) VALUES
('Thông báo nghỉ lễ Tết Nguyên Đán 2026', 
 'Kính gửi các em học sinh và quý phụ huynh! Thư viện sẽ nghỉ Tết Nguyên Đán từ ngày 26/01/2026 đến hết 02/02/2026. Các em nhớ trả sách trước ngày 25/01 để tránh bị tính phạt trễ hạn nhé. Chúc các em và gia đình năm mới an khang, thịnh vượng!',
 NOW(), NOW()),

('Ngày hội "Đọc sách - Lan tỏa tri thức" năm 2026',
 'Thư viện trường THCS Phương Tú tổ chức Ngày hội "Đọc sách - Lan tỏa tri thức" vào ngày 15/02/2026. Chương trình có nhiều hoạt động hấp dẫn: thi viết bài cảm nhận về sách, giao lưu với tác giả, trưng bày sách quý hiếm. Tất cả học sinh đều được khuyến khích tham gia. Phần thưởng giá trị dành cho các bạn xuất sắc!',
 NOW(), NOW()),

('Vinh danh Top 10 độc giả xuất sắc tháng 12/2025',
 'Xin chúc mừng 10 bạn học sinh có thành tích đọc sách xuất sắc nhất tháng 12/2025: Nguyễn Văn A (8A1), Trần Thị B (9B2), Lê Văn C (7C3), và các bạn khác. Mỗi bạn nhận được 1 phiếu quà tặng sách trị giá 100.000đ và chứng nhận khen thưởng. Hẹn gặp các bạn tại buổi trao thưởng ngày 10/01/2026!',
 NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),

('Bổ sung 500 đầu sách mới phục vụ học kỳ II',
 'Thư viện vừa nhập về 500 đầu sách mới bao gồm: sách giáo khoa bổ trợ, sách tham khảo môn Toán - Lý - Hóa, truyện thiếu nhi, văn học Việt Nam và nước ngoài. Đặc biệt có bộ "Thám tử lừng danh Conan" mới nhất và series "Harry Potter" bản tiếng Việt. Các em có thể đến mượn từ ngày 08/01/2026!',
 NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY),

('Hướng dẫn sử dụng hệ thống mượn trả sách trực tuyến',
 'Từ tháng 01/2026, thư viện triển khai hệ thống mượn trả sách trực tuyến hoàn toàn mới. Các em có thể tìm kiếm, đặt sách trước, xem lịch sử mượn, và nhận thông báo hết hạn qua website. Đăng nhập bằng tài khoản học sinh, mật khẩu mặc định là ngày sinh (ddmmyyyy). Mọi thắc mắc vui lòng liên hệ cô thủ thư hoặc comment bên dưới!',
 NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY);
