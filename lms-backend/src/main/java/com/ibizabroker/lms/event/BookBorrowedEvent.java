package com.ibizabroker.lms.event;

import java.time.LocalDate;

/**
 * Domain Event: fired after a book is successfully borrowed.
 */
public record BookBorrowedEvent(
        Integer loanId,
        Integer bookId,
        String  bookName,
        Integer memberId,
        String  memberEmail,
        String  memberName,
        LocalDate loanDate,
        LocalDate dueDate
) {}
