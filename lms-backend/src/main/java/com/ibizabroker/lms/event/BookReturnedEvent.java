package com.ibizabroker.lms.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain Event: fired after a book is successfully returned.
 * CirculationService publishes this; downstream services listen asynchronously.
 */
public record BookReturnedEvent(
        Integer loanId,
        Integer bookId,
        String  bookName,
        Integer memberId,
        String  memberEmail,
        String  memberName,
        LocalDate dueDate,
        LocalDate returnDate,
        long    overdueDays,
        BigDecimal fineAmount
) {}
