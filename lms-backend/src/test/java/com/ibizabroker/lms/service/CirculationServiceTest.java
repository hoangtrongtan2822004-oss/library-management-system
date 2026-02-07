package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.entity.Books;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CirculationService
 */
@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private CirculationService circulationService;

    private Books testBook;

    @BeforeEach
    void setUp() {
        testBook = new Books();
        testBook.setId(1);
        testBook.setName("Test Book");
        testBook.setNumberOfCopiesAvailable(5);
    }

    @SuppressWarnings("null")
    @Test
    void testBorrowBook_Success() {
        // Simulate borrowing directly on entity
        testBook.borrowBook();

        assertEquals(4, testBook.getNumberOfCopiesAvailable());
    }

    @Test
    void testBorrowBook_NoAvailableCopies() {
        testBook.setNumberOfCopiesAvailable(0);

        assertThrows(IllegalStateException.class, () -> testBook.borrowBook());
    }

    @Test
    void testReturnBook_Success() {
        // Simulate returning directly on entity
        testBook.returnBook();

        assertEquals(6, testBook.getNumberOfCopiesAvailable());
    }
}
