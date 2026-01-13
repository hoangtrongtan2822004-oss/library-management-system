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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    void testBorrowBook_Success() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(testBook));
        when(booksRepository.save(any(Books.class))).thenReturn(testBook);

        // Simulate borrowing
        testBook.borrowBook();

        assertEquals(4, testBook.getNumberOfCopiesAvailable());
        verify(booksRepository).findById(1);
    }

    @Test
    void testBorrowBook_NoAvailableCopies() {
        testBook.setNumberOfCopiesAvailable(0);

        assertThrows(IllegalStateException.class, () -> testBook.borrowBook());
    }

    @Test
    void testReturnBook_Success() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(testBook));

        // Simulate returning
        testBook.returnBook();

        assertEquals(6, testBook.getNumberOfCopiesAvailable());
        verify(booksRepository).findById(1);
    }
}
