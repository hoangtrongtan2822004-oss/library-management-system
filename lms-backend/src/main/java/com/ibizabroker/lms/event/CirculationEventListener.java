package com.ibizabroker.lms.event;

import com.ibizabroker.lms.dto.NotificationDto;
import com.ibizabroker.lms.entity.AuditLogEntry;
import com.ibizabroker.lms.service.AuditLogService;
import com.ibizabroker.lms.service.EmailService;
import com.ibizabroker.lms.service.GamificationService;
import com.ibizabroker.lms.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 📡 Circulation Event Listener
 *
 * Pub/Sub pattern: CirculationService publishes domain events; this listener
 * reacts to them asynchronously so the main API response is not blocked.
 *
 * Benefits:
 *  - returnBook API is 3-5× faster (no inline email/websocket/gamification)
 *  - Email failures do NOT roll back the return transaction
 *  - Each concern is isolated and testable independently
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CirculationEventListener {

    private final GamificationService gamificationService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final WebSocketNotificationService wsNotificationService;

    // ─────────────────── BOOK RETURNED ───────────────────

    @Async
    @EventListener
    public void onBookReturned(BookReturnedEvent event) {
        log.info("[Event] BookReturned loanId={} memberId={} overdueDays={}",
                event.loanId(), event.memberId(), event.overdueDays());

        awardReturnPoints(event);
        sendReturnAuditLog(event);
        sendReturnWebSocket(event);
        sendReturnEmail(event);
    }

    private void awardReturnPoints(BookReturnedEvent event) {
        try {
            if (event.overdueDays() == 0) {
                gamificationService.onBookReturnedOnTime(event.memberId());
                log.debug("[Gamification] Awarded return-on-time points to memberId={}", event.memberId());
            }
        } catch (Exception e) {
            log.error("[Gamification] Failed to award points for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private void sendReturnAuditLog(BookReturnedEvent event) {
        try {
            AuditLogEntry entry = new AuditLogEntry();
            entry.setActor(event.memberName());
            entry.setAction("RETURN_BOOK");
            entry.setResource("Loan");
            entry.setTargetId(String.valueOf(event.loanId()));
            entry.setRequestPayload(String.format("Trả sách '%s' (loanId=%d, quá hạn=%d ngày, phạt=%s VNĐ)",
                    event.bookName(), event.loanId(), event.overdueDays(),
                    event.fineAmount() != null ? event.fineAmount().toPlainString() : "0"));
            entry.setStatus(event.overdueDays() > 0 ? "OVERDUE" : "ON_TIME");
            entry.setCreatedAt(LocalDateTime.now());
            auditLogService.save(entry);
        } catch (Exception e) {
            log.error("[AuditLog] Failed to write audit for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private void sendReturnWebSocket(BookReturnedEvent event) {
        try {
            String message = event.overdueDays() > 0
                    ? String.format("Bạn đã trả sách '%s'. Phí phạt: %s VNĐ (quá hạn %d ngày).",
                        event.bookName(),
                        event.fineAmount() != null ? event.fineAmount().toPlainString() : "0",
                        event.overdueDays())
                    : String.format("Bạn đã trả sách '%s' đúng hạn. +15 điểm!", event.bookName());

            NotificationDto notification = event.overdueDays() > 0
                    ? NotificationDto.warning("Trả sách – Có phí phạt", message)
                    : NotificationDto.success("Trả sách thành công", message);

            wsNotificationService.sendToUser(event.memberName(), notification);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to send notification for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private void sendReturnEmail(BookReturnedEvent event) {
        try {
            if (event.memberEmail() == null || event.memberEmail().isBlank()) return;

            String subject = event.overdueDays() > 0
                    ? "[Thư viện] Xác nhận trả sách – Có phí phạt"
                    : "[Thư viện] Xác nhận trả sách thành công";

            String html = buildReturnEmailHtml(event);
            emailService.sendHtmlMessage(event.memberEmail(), subject, html);
        } catch (Exception e) {
            log.error("[Email] Failed to send return email for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private String buildReturnEmailHtml(BookReturnedEvent event) {
        String fineBlock = event.overdueDays() > 0
                ? String.format("<p style='color:#c0392b;'>⚠️ Quá hạn <b>%d ngày</b> – Phí phạt: <b>%s VNĐ</b></p>",
                    event.overdueDays(),
                    event.fineAmount() != null ? event.fineAmount().toPlainString() : "0")
                : "<p style='color:#27ae60;'>✅ Trả đúng hạn – Bạn được cộng <b>+15 điểm</b>!</p>";

        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <h2 style="color:#2c3e50;">📚 Xác nhận trả sách</h2>
              <p>Xin chào <b>%s</b>,</p>
              <table style="border-collapse:collapse;width:100%%;">
                <tr><td style="padding:8px;border:1px solid #ddd;"><b>Sách</b></td><td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><b>Ngày trả</b></td><td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><b>Hạn trả</b></td><td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
              </table>
              %s
              <p style="color:#7f8c8d;font-size:12px;">Email tự động từ Hệ thống Thư viện.</p>
            </div>
            """.formatted(
                event.memberName(),
                event.bookName(),
                event.returnDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                event.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                fineBlock
            );
    }

    // ─────────────────── BOOK BORROWED ───────────────────

    @Async
    @EventListener
    public void onBookBorrowed(BookBorrowedEvent event) {
        log.info("[Event] BookBorrowed loanId={} memberId={}", event.loanId(), event.memberId());

        awardBorrowPoints(event);
        sendBorrowAuditLog(event);
        sendBorrowEmail(event);
    }

    private void awardBorrowPoints(BookBorrowedEvent event) {
        try {
            gamificationService.onBookBorrowed(event.memberId());
            log.debug("[Gamification] Awarded borrow points to memberId={}", event.memberId());
        } catch (Exception e) {
            log.error("[Gamification] Failed to award borrow points for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private void sendBorrowAuditLog(BookBorrowedEvent event) {
        try {
            AuditLogEntry entry = new AuditLogEntry();
            entry.setActor(event.memberName());
            entry.setAction("BORROW_BOOK");
            entry.setResource("Loan");
            entry.setTargetId(String.valueOf(event.loanId()));
            entry.setRequestPayload(String.format("Mượn sách '%s' (loanId=%d, hạn trả=%s)",
                    event.bookName(), event.loanId(),
                    event.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            entry.setStatus("SUCCESS");
            entry.setCreatedAt(LocalDateTime.now());
            auditLogService.save(entry);
        } catch (Exception e) {
            log.error("[AuditLog] Failed to write borrow audit for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }

    private void sendBorrowEmail(BookBorrowedEvent event) {
        try {
            if (event.memberEmail() == null || event.memberEmail().isBlank()) return;
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#2c3e50;">📖 Xác nhận mượn sách</h2>
                  <p>Xin chào <b>%s</b>, bạn đã mượn sách thành công!</p>
                  <table style="border-collapse:collapse;width:100%%;">
                    <tr><td style="padding:8px;border:1px solid #ddd;"><b>Sách</b></td><td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><b>Ngày mượn</b></td><td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><b>Hạn trả</b></td><td style="padding:8px;border:1px solid #ddd;color:#e74c3c;"><b>%s</b></td></tr>
                  </table>
                  <p style="color:#27ae60;">+10 điểm đã được cộng vào tài khoản của bạn!</p>
                  <p style="color:#7f8c8d;font-size:12px;">Email tự động từ Hệ thống Thư viện.</p>
                </div>
                """.formatted(
                    event.memberName(),
                    event.bookName(),
                    event.loanDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    event.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
            emailService.sendHtmlMessage(event.memberEmail(), "[Thư viện] Xác nhận mượn sách", html);
        } catch (Exception e) {
            log.error("[Email] Failed to send borrow email for loanId={}: {}", event.loanId(), e.getMessage());
        }
    }
}
