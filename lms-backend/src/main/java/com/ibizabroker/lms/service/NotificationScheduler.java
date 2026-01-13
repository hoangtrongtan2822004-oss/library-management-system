package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.entity.LoanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final LoanRepository loanRepository;
    private final UsersRepository usersRepository;
    private final EmailService emailService;

    // Chạy vào 8 giờ sáng mỗi ngày
    @SuppressWarnings("null")
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDueDateReminders() {
        logger.info("🔔 Running scheduled task: Sending due date reminders...");

        // Gửi nhắc nhở cho sách sắp hết hạn (còn 2 ngày)
        LocalDate dueDateInTwoDays = LocalDate.now().plusDays(2);
        List<Loan> upcomingLoans = loanRepository.findByStatusAndDueDate(LoanStatus.ACTIVE, dueDateInTwoDays);

        for (Loan loan : upcomingLoans) {
            usersRepository.findById(loan.getMemberId()).ifPresent(user -> {
                if (user.getEmail() != null) {
                    String subject = "[LMS] Nhắc nhở: Sách sắp đến hạn trả";
                    String text = String.format(
                        "Chào %s,\n\nCuốn sách bạn mượn sẽ hết hạn trong 2 ngày nữa (vào ngày %s).\n\nVui lòng trả sách đúng hạn hoặc gia hạn nếu cần.\n\nTrân trọng,\nThư viện LMS.",
                        user.getName(), loan.getDueDate()
                    );
                    emailService.sendSimpleMessage(user.getEmail(), subject, text);
                }
            });
        }
    }
}