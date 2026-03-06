package com.ibizabroker.lms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendHtmlMessage() {
        String to = "trngtn2824@gmail.com";
        String subject = "Thông báo mượn/trả sách - LMS";
        String html = """
        <html>
        <body style='font-family:Arial;background:#f8f9fa;color:#222;'>
            <h2 style='color:#007bff;'>Thông báo mượn/trả sách</h2>
            <p>Xin chào <b>Bạn đọc</b>,</p>
            <p>Bạn vừa mượn sách <b style='color:#28a745;'>"Dế Mèn Phiêu Lưu Ký"</b> từ thư viện trường.</p>
            <p>Ngày mượn: <b>30/11/2025</b></p>
            <p>Ngày trả dự kiến: <b>07/12/2025</b></p>
            <hr>
            <p style='color:#dc3545;'>Vui lòng trả sách đúng hạn để tránh bị phạt!</p>
            <p>Trân trọng,<br>Thư viện THCS Phương Tú</p>
        </body>
        </html>
        """;
        emailService.sendHtmlMessage(to, subject, html);
    }
}
