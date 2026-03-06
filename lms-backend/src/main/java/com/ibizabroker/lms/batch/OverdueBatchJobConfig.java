package com.ibizabroker.lms.batch;

import com.ibizabroker.lms.entity.FineStatus;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.entity.LoanStatus;
import com.ibizabroker.lms.service.SystemSettingService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 🗓️ Spring Batch – Nightly Overdue Processing Job
 *
 * Architecture:
 *   Reader  → JpaPagingItemReader reads ACTIVE loans with dueDate < today (100 rows/chunk)
 *   Processor → marks OVERDUE, recalculates fine (finePerDay × overdueDays)
 *   Writer  → JpaItemWriter bulk-saves updated loans
 *
 * Scheduled at 00:00 every night by {@link OverdueBatchScheduler}.
 * Replaces the naive @Scheduled full-table scan; handles millions of records safely.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("null") // Spring Batch builder APIs lack @NonNull annotations; all usages are safe
public class OverdueBatchJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final EntityManagerFactory entityManagerFactory;
    private final SystemSettingService systemSettingService;

    // ── Reader ──────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public JpaPagingItemReader<Loan> overdueLoansReader() {
        return new JpaPagingItemReaderBuilder<Loan>()
                .name("overdueLoansReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                    "SELECT l FROM Loan l " +
                    "WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
                    "AND l.dueDate < :today " +
                    "ORDER BY l.id")
                .parameterValues(Map.of("today", LocalDate.now()))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    // ── Processor ────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public ItemProcessor<Loan, Loan> overdueProcessor() {
        return loan -> {
            LocalDate today = LocalDate.now();
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            if (overdueDays <= 0) return null; // skip (should not happen but be safe)

            BigDecimal finePerDay = systemSettingService.getBigDecimal(
                    SystemSettingService.KEY_FINE_PER_DAY, new BigDecimal("2000"));
            BigDecimal newFine = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

            boolean changed = loan.getStatus() != LoanStatus.OVERDUE
                    || !newFine.equals(loan.getFineAmount());

            loan.setStatus(LoanStatus.OVERDUE);
            loan.setFineAmount(newFine);
            if (loan.getFineStatus() == FineStatus.NO_FINE) {
                loan.setFineStatus(FineStatus.UNPAID);
            }

            if (changed) {
                log.debug("[Batch] loanId={} overdueDays={} fine={}", loan.getId(), overdueDays, newFine);
            }
            return loan;
        };
    }

    // ── Writer ───────────────────────────────────────────────────────────────

    @Bean
    public JpaItemWriter<Loan> overdueLoansWriter() {
        JpaItemWriter<Loan> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // ── Step & Job ───────────────────────────────────────────────────────────

    @Bean
    public Step overdueProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("overdueProcessingStep", jobRepository)
                .<Loan, Loan>chunk(CHUNK_SIZE, transactionManager)
                .reader(overdueLoansReader())
                .processor(overdueProcessor())
                .writer(overdueLoansWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Job overdueProcessingJob(
            JobRepository jobRepository,
            Step overdueProcessingStep) {
        return new JobBuilder("overdueProcessingJob", jobRepository)
                .start(overdueProcessingStep)
                .build();
    }
}
