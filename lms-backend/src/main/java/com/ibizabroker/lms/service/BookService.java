package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.AuthorRepository;
import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.CategoryRepository;
import com.ibizabroker.lms.dto.BookCreateDto;
import com.ibizabroker.lms.dto.BookUpdateDto;
import com.ibizabroker.lms.entity.Author;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Category;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BooksRepository booksRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    // @Cacheable(value = "book-details", key = "'all'") // DISABLED: Causes ClassCastException with LinkedHashMap
    public List<Books> getAllBooks() {
        return booksRepository.findAll();
    }

    @Transactional(readOnly = true)
    // @Cacheable(value = "book-details", key = "#id") // DISABLED: Causes ClassCastException with LinkedHashMap
    public Books getBookById(Integer id) {
        return booksRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book with id " + id + " does not exist."));
    }
    
    @Transactional(readOnly = true)
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @CacheEvict(value = {
            "book-details",
            "books-newest",
            "similar-books",
            "featured-books",
            "search-suggestions",
            "author-suggestions"
    }, allEntries = true)
    public Books createBook(BookCreateDto dto) {
        Books book = new Books();
        book.setName(dto.getName());
        book.setNumberOfCopiesAvailable(dto.getNumberOfCopiesAvailable());
        book.setPublishedYear(dto.getPublishedYear());
        book.setIsbn(dto.getIsbn());
        book.setCoverUrl(dto.getCoverUrl());

        Set<Author> authors = authorRepository.findByIdIn(dto.getAuthorIds());
        if(authors.size() != dto.getAuthorIds().size()) {
            throw new NotFoundException("Một hoặc nhiều ID tác giả không tìm thấy.");
        }
        book.setAuthors(authors);

        Set<Category> categories = categoryRepository.findByIdIn(dto.getCategoryIds());
         if(categories.size() != dto.getCategoryIds().size()) {
            throw new NotFoundException("Một hoặc nhiều ID thể loại không tìm thấy.");
        }
        book.setCategories(categories);

        Books saved = booksRepository.save(book);
        return saved;
    }

    @CacheEvict(value = {
            "book-details",
            "books-newest",
            "similar-books",
            "featured-books",
            "search-suggestions",
            "author-suggestions"
    }, allEntries = true)
    public Books updateBook(Integer id, BookUpdateDto dto) {
        Books book = getBookById(id);

        if (dto.getName() != null) book.setName(dto.getName());
        if (dto.getNumberOfCopiesAvailable() != null) book.setNumberOfCopiesAvailable(dto.getNumberOfCopiesAvailable());
        if (dto.getPublishedYear() != null) book.setPublishedYear(dto.getPublishedYear());
        if (dto.getIsbn() != null) book.setIsbn(dto.getIsbn());
        if (dto.getCoverUrl() != null) book.setCoverUrl(dto.getCoverUrl());

        if (dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty()) {
            Set<Author> authors = authorRepository.findByIdIn(dto.getAuthorIds());
            book.setAuthors(authors);
        }
        
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            Set<Category> categories = categoryRepository.findByIdIn(dto.getCategoryIds());
            book.setCategories(categories);
        }
        
        @SuppressWarnings("null")
        Books saved = booksRepository.save(book);
        return saved;
    }

    @SuppressWarnings("null")
    @CacheEvict(value = {
            "book-details",
            "books-filtered",
            "books-newest",
            "similar-books",
            "featured-books",
            "search-suggestions",
            "author-suggestions"
    }, allEntries = true)
    public void deleteBook(Integer id) {
        Books book = getBookById(id);
        booksRepository.delete(book);
    }

    @Transactional(readOnly = true)
    public Page<Books> findBooksWithFilters(String search, String genre, Boolean availableOnly, Pageable pageable) {
        boolean isAvailableOnly = availableOnly != null && availableOnly;
        return booksRepository.findWithFiltersAndPagination(search, genre, isAvailableOnly, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "books-newest", key = "#pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()")
    public List<Books> getNewestBooks(Pageable pageable) {
        return booksRepository.findNewestBooks(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "featured-books", key = "'top-10'")
    public List<Books> getFeaturedBooks() {
        return booksRepository.findNewestBooks(PageRequest.of(0, 10));
    }
}