package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BooksRepository booksRepository;

    @InjectMocks
    private BookService bookService;

    private Books book1;

    @BeforeEach
    void setUp() {
        book1 = new Books();
        book1.setId(1);
        book1.setName("Spring Boot in Action");
        book1.setIsbn("978-1617292545");
        book1.setNumberOfCopiesAvailable(7);
    }

    @Test
    void testGetBookById_Success() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book1));

        Books result = bookService.getBookById(1);

        assertNotNull(result);
        assertEquals("Spring Boot in Action", result.getName());
        verify(booksRepository).findById(1);
    }

    @Test
    void testGetBookById_NotFound() {
        when(booksRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.getBookById(999));
        verify(booksRepository).findById(999);
    }

    @Test
    void testGetAllBooks() {
        List<Books> books = Arrays.asList(book1);
        when(booksRepository.findAll()).thenReturn(books);

        List<Books> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        verify(booksRepository).findAll();
    }

    @SuppressWarnings("null")
    @Test
    void testDeleteBook_Success() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book1));
        doNothing().when(booksRepository).delete(any(Books.class));

        bookService.deleteBook(1);

        verify(booksRepository).findById(1);
        // ✅ Sửa: Service gọi delete(entity) chứ không phải deleteById(id)
        verify(booksRepository).delete(any(Books.class));
    }
}
