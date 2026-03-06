package com.ibizabroker.lms.batch;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.entity.LoanStatus;
import com.ibizabroker.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ⏰ Overdue Batch Scheduler
 *
 * Runs two nightly jobs:
 *  1. 00:00 — overdueProcessingJob  → marks ACTIVE loans past due date as OVERDUE,
 *                                      recalculates fine for all overdue loans (chunk 100)
 *  2. 00:05 — overdueEmailReminders → sends email to members whose due date is TODAY
 *                                      (uses EmailService which is already @Async)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null") // Spring Batch / JPA APIs lack @NonNull annotations; all usages are safe
public class OverdueBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job overdueProcessingJob;
    private final LoanRepository loanRepository;
    private final UsersRepository usersRepository;
    private final EmailService emailService;

    /**
     * Nightly overdue scan at 00:00.
     * Spring Batch handles chunking (100 rows/tx) and skip/retry.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runOverdueProcessingJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDate("runDate", LocalDate.now())
                    .toJobParameters();
            log.info("[Batch] Starting overdueProcessingJob for date={}", LocalDate.now());
            var execution = jobLauncher.run(overdueProcessingJob, params);
            log.info("[Batch] overdueProcessingJob finished: status={} writeCount={}",
                    execution.getStatus(),
                    execution.getStepExecutions().stream()
                            .mapToLong(s -> s.getWriteCount()).sum());
        } catch (Exception e) {
            log.error("[Batch] overdueProcessingJob failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Send due-date reminder emails at 00:05 (after fine job finishes).
     * Paginated to avoid loading millions of records into memory.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void sendDueDateReminderEmails() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("[Batch] Sending due-date reminders for dueDate={}", tomorrow);

        int page = 0;
        int processed = 0;
        Page<Loan> batch;
        do {
            batch = loanRepository.findByStatusAndDueDateBetween(
                    LoanStatus.ACTIVE, tomorrow, tomorrow, PageRequest.of(page, 200));
            for (Loan loan : batch.getContent()) {
                try {
                    var member = usersRepository.findById(loan.getMemberId()).orElse(null);
                    if (member == null || member.getEmail() == null || member.getEmail().isBlank()) continue;

                    String memberName = member.getName() != null ? member.getName() : member.getUsername();
                    String bookName   = loan.getBook() != null ? loan.getBook().getName() : "sách đã mượn";
                    String html = buildReminderEmail(memberName, bookName, loan.getDueDate());
                    emailService.sendHtmlMessage(member.getEmail(), "[Thư viện] Nhắc nhở: Sách đến hạn trả ngày mai", html);
                    processed++;
                } catch (Exception e) {
                    log.error("[Batch] Reminder email failed for loanId={}: {}", loan.getId(), e.getMessage());
                }
            }
            page++;
        } while (batch.hasNext());

        log.info("[Batch] Due-date reminders sent: {} emails", processed);
    }

    private String buildReminderEmail(String memberName, String bookName, LocalDate dueDate) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <h2 style="color:#e67e22;">⏰ Nhắc nhở: Sách đến hạn trả</h2>
              <p>Xin chào <b>%s</b>,</p>
              <p>Sách <b>"%s"</b> của bạn sẽ <b>đến hạn trả vào ngày mai (%s)</b>.</p>
              <p>Vui lòng trả sách đúng hạn để tránh phí phạt và duy trì điểm thưởng của bạn.</p>
              <div style="background:#fef9e7;border-left:4px solid #e67e22;padding:12px;margin:16px 0;">
                <b>📌 Lưu ý:</b> Phí phạt sẽ được tính từ ngày quá hạn.
              </div>
              <p style="color:#7f8c8d;font-size:12px;">Email tự động từ Hệ thống Thư viện.</p>
            </div>
            """.formatted(
                memberName, bookName,
                dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
    }
}
