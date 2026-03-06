package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.AuthorRepository;
import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.CategoryRepository;
import com.ibizabroker.lms.dao.LoanRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BooksRepository booksRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final LoanRepository loanRepository;
    private final EmbeddingService embeddingService;
    private final PineconeVectorStore pineconeVectorStore;
    private final AiTaggingService aiTaggingService;

    /**
     * ⚠️ DEPRECATED: Use getAllBooks(Pageable) instead
     * 
     * 🚨 Performance Issue:
     * - Returns ALL books from database (no limit)
     * - 10,000 books × 2KB = 20MB payload
     * - Can crash frontend browser
     * - High memory usage on backend
     * 
     * ✅ Migration Path:
     * - Use: getAllBooks(PageRequest.of(0, 20)) for paginated results
     * - Default page size: 20 items
     * 
     * @deprecated Since Phase 8, will be removed in future version. Use {@link #getAllBooks(Pageable)} instead.
     */
    @Deprecated(since = "Phase 8", forRemoval = true)
    @Transactional(readOnly = true)
    // @Cacheable(value = "book-details", key = "'all'") // DISABLED: Causes ClassCastException with LinkedHashMap
    public List<Books> getAllBooks() {
        // ⚠️ Return only first 100 books to prevent payload bomb
        return booksRepository.findAll(PageRequest.of(0, 100)).getContent();
    }

    /**
     * ✅ Get all books with pagination (RECOMMENDED)
     * 
     * 🎯 Performance:
     * - Paginated results: Fetch only requested page
     * - Controllable payload size
     * - Frontend-friendly (supports page navigation)
     * 
     * 📌 Example Usage:
     * - First page (20 items): getAllBooks(PageRequest.of(0, 20))
     * - Second page: getAllBooks(PageRequest.of(1, 20))
     * - Custom sort: getAllBooks(PageRequest.of(0, 20, Sort.by("name")))
     * 
     * @param pageable Pagination parameters (page number, page size, sort)
     * @return Page object containing books and pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<Books> getAllBooks(Pageable pageable) {
        return booksRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    // @Cacheable(value = "book-details", key = "#id") // DISABLED: Causes ClassCastException with LinkedHashMap
    public Books getBookById(Integer id) {
        return booksRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book with id " + id + " does not exist."));
    }
    
    @Transactional(readOnly = true)
    public List<Books> getBooksByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        // JpaRepository provides findAllById which accepts Iterable<Integer>
        return booksRepository.findAllById(ids).stream().toList();
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
        book.setShelfCode(dto.getShelfCode());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());

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
        CompletableFuture.runAsync(() -> indexBook(saved));
        // Async AI auto-tagging after save
        scheduleAiTagging(saved);
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
        if (dto.getShelfCode() != null) book.setShelfCode(dto.getShelfCode());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());

        boolean tagsStale = dto.getName() != null || dto.getDescription() != null
                || (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty());

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
        CompletableFuture.runAsync(() -> indexBook(saved));
        // Re-generate AI tags when name/description/categories change
        if (tagsStale) scheduleAiTagging(saved);
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
        CompletableFuture.runAsync(() -> pineconeVectorStore.delete(String.valueOf(id)));
    }

    /**
     * 🏷️ Async AI auto-tagging — fires after save, result stored back to DB.
     */
    private void scheduleAiTagging(Books book) {
        List<String> catNames = book.getCategories() == null ? List.of()
                : book.getCategories().stream().map(Category::getName).toList();
        String desc = book.getDescription() != null ? book.getDescription() : "";
        aiTaggingService.generateTagsAsync(book.getName(), desc, catNames)
            .thenAccept(tagsJson -> {
                try {
                    booksRepository.findById(book.getId()).ifPresent(b -> {
                        b.setAiTags(tagsJson);
                        booksRepository.save(b);
                        log.info("AI tags saved for book {}: {}", b.getId(), tagsJson);
                        // Re-index with enriched tags so Pinecone benefits from them
                        indexBook(b);
                    });
                } catch (Exception e) {
                    log.warn("Failed to persist AI tags for book {}: {}", book.getId(), e.getMessage());
                }
            });
    }

    /**
     * 🔄 Index a single book into Pinecone (called after create/update).
     * Runs asynchronously — never blocks the API response.
     */
    private void indexBook(Books book) {
        try {
            StringBuilder text = new StringBuilder(book.getName());
            if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                text.append(" ").append(
                    book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(" "))
                );
            }
            if (book.getCategories() != null && !book.getCategories().isEmpty()) {
                text.append(" ").append(
                    book.getCategories().stream().map(Category::getName).collect(Collectors.joining(" "))
                );
            }
            if (book.getDescription() != null && !book.getDescription().isBlank()) {
                text.append(" ").append(book.getDescription());
            }
            // Include AI-generated tags to enrich semantic search
            List<String> aiTags = aiTaggingService.parseTags(book.getAiTags());
            if (!aiTags.isEmpty()) {
                text.append(" ").append(String.join(" ", aiTags));
            }
            List<Double> vector = embeddingService.embed(text.toString());
            if (vector != null && !vector.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", book.getName());
                pineconeVectorStore.upsert(String.valueOf(book.getId()), vector, metadata);
                log.debug("Pinecone upsert OK: bookId={} textLen={}", book.getId(), text.length());
            }
        } catch (Exception ignored) {
            // Indexing is best-effort — never fail the main operation
        }
    }

    /**
     * 🔄 Bulk reindex all books into Pinecone.
     * Call once after configuring Pinecone API keys to seed the index with existing books.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> reindexAllBooks() {
        List<Books> all = booksRepository.findAll();
        int indexed = 0;
        int failed = 0;
        log.info("Starting reindex of {} books into Pinecone...", all.size());
        for (int i = 0; i < all.size(); i++) {
            Books book = all.get(i);
            try {
                StringBuilder text = new StringBuilder(book.getName());
                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    text.append(" ").append(
                        book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(" "))
                    );
                }
                if (book.getCategories() != null && !book.getCategories().isEmpty()) {
                    text.append(" ").append(
                        book.getCategories().stream().map(Category::getName).collect(Collectors.joining(" "))
                    );
                }
                // Include description (consistent with indexBook)
                if (book.getDescription() != null && !book.getDescription().isBlank()) {
                    text.append(" ").append(book.getDescription());
                }
                // Include AI tags (consistent with indexBook)
                List<String> tags = aiTaggingService.parseTags(book.getAiTags());
                if (!tags.isEmpty()) {
                    text.append(" ").append(String.join(" ", tags));
                }
                List<Double> vector = embeddingService.embed(text.toString());
                if (vector != null && !vector.isEmpty()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("name", book.getName());
                    pineconeVectorStore.upsert(String.valueOf(book.getId()), vector, metadata);
                    indexed++;
                    log.info("Reindex [{}/{}] OK: bookId={} name={}", i + 1, all.size(), book.getId(), book.getName());
                } else {
                    failed++;
                    log.warn("Reindex [{}/{}] FAILED (empty vector): bookId={} name={}", i + 1, all.size(), book.getId(), book.getName());
                }
                // Respect Gemini free tier rate limit (~15 RPM); 250 ms ≈ 4 req/s
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Reindex interrupted at bookId={}", book.getId());
                break;
            } catch (Exception e) {
                failed++;
                log.error("Reindex [{}/{}] ERROR: bookId={} error={}", i + 1, all.size(), book.getId(), e.getMessage());
            }
        }
        log.info("Reindex complete: total={} indexed={} failed={}", all.size(), indexed, failed);
        Map<String, Object> result = new HashMap<>();
        result.put("total", all.size());
        result.put("indexed", indexed);
        result.put("failed", failed);
        return result;
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

    @Transactional(readOnly = true)
    public List<Books> getSimilarBooks(Integer bookId, int limit) {
        Books book = getBookById(bookId);
        List<Integer> categoryIds = book.getCategories().stream()
                .map(Category::getId)
                .toList();

        if (categoryIds.isEmpty()) {
            return getNewestBooks(PageRequest.of(0, limit));
        }

        return booksRepository.findSimilarBooks(bookId, categoryIds, PageRequest.of(0, limit));
    }

    /**
     * 🏷️ Return parsed AI tags for a book (empty list if none generated yet).
     */
    @Transactional(readOnly = true)
    public List<String> getAiTags(Integer bookId) {
        Books book = getBookById(bookId);
        return aiTaggingService.parseTags(book.getAiTags());
    }

    /**
     * 🤖 AI-powered similarity: embeds book text → queries Pinecone for nearest vectors.
     * Falls back to category-based if Pinecone is not configured or returns empty.
     */
    @Transactional(readOnly = true)
    public List<Books> getAiSimilarBooks(Integer bookId, int limit) {
        try {
            Books book = getBookById(bookId);

            // Build rich text for embedding (nhất quán với indexBook())
            StringBuilder text = new StringBuilder(book.getName());
            if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                text.append(" ").append(
                    book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(" "))
                );
            }
            if (book.getCategories() != null && !book.getCategories().isEmpty()) {
                text.append(" ").append(
                    book.getCategories().stream().map(Category::getName).collect(Collectors.joining(" "))
                );
            }
            if (book.getDescription() != null && !book.getDescription().isBlank()) {
                String desc = book.getDescription().length() > 200
                    ? book.getDescription().substring(0, 200)
                    : book.getDescription();
                text.append(" ").append(desc);
            }
            List<String> aiTags = aiTaggingService.parseTags(book.getAiTags());
            if (!aiTags.isEmpty()) {
                text.append(" ").append(String.join(" ", aiTags));
            }

            List<Double> vector = embeddingService.embed(text.toString());
            if (vector == null || vector.isEmpty()) {
                return getSimilarBooks(bookId, limit);
            }

            // Query limit+1 to exclude self
            List<String> nearestIds = pineconeVectorStore.query(vector, limit + 1);
            if (nearestIds == null || nearestIds.isEmpty()) {
                return getSimilarBooks(bookId, limit);
            }

            List<Integer> intIds = nearestIds.stream()
                    .map(id -> { try { return Integer.parseInt(id); } catch (NumberFormatException e) { return null; } })
                    .filter(id -> id != null && !id.equals(bookId))
                    .limit(limit)
                    .toList();

            if (intIds.isEmpty()) {
                return getSimilarBooks(bookId, limit);
            }

            List<Books> result = booksRepository.findAllById(intIds);
            return result.isEmpty() ? getSimilarBooks(bookId, limit) : result;
        } catch (Exception e) {
            return getSimilarBooks(bookId, limit);
        }
    }

    @Transactional(readOnly = true)
    public List<Books> getRecommendationsForUser(Integer userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        List<Integer> topCategoryIds = loanRepository.findTopCategoryIdsByMemberId(
                userId,
                PageRequest.of(0, 3));

        if (topCategoryIds == null || topCategoryIds.isEmpty()) {
            return getNewestBooks(PageRequest.of(0, limit));
        }

        List<Integer> excludeBookIds = loanRepository.findDistinctBookIdsByMemberId(userId);
        List<Books> candidates;

        if (excludeBookIds == null || excludeBookIds.isEmpty()) {
            candidates = booksRepository.findByCategoryIds(
                    topCategoryIds,
                    PageRequest.of(0, limit));
        } else {
            candidates = booksRepository.findByCategoryIdsExcludingBookIds(
                    topCategoryIds,
                    excludeBookIds,
                    PageRequest.of(0, limit));
        }

        if (candidates == null || candidates.isEmpty()) {
            return getNewestBooks(PageRequest.of(0, limit));
        }

        return candidates;
    }

    // =============================================
    // Category CRUD
    // =============================================

    public Category createCategory(String name, Integer parentId, String color, String iconClass) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalStateException("Tên danh mục không được để trống");
        if (categoryRepository.existsByName(trimmed)) {
            throw new IllegalStateException("Danh mục '" + trimmed + "' đã tồn tại");
        }
        Category cat = new Category();
        cat.setName(trimmed);
        cat.setParentId(parentId);
        cat.setColor(color);
        cat.setIconClass(iconClass);
        return categoryRepository.save(cat);
    }

    public Category updateCategory(Integer id, String name) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        if (name != null && !name.trim().isEmpty()) {
            if (categoryRepository.existsByNameAndIdNot(name.trim(), id)) {
                throw new IllegalStateException("Danh mục '" + name.trim() + "' đã tồn tại");
            }
            cat.setName(name.trim());
        }
        return categoryRepository.save(cat);
    }

    public Category updateCategoryFull(Integer id, java.util.Map<String, Object> fields) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        if (fields.containsKey("name") && fields.get("name") != null) {
            String newName = fields.get("name").toString().trim();
            if (!newName.isEmpty()) {
                if (categoryRepository.existsByNameAndIdNot(newName, id)) {
                    throw new IllegalStateException("Danh mục '" + newName + "' đã tồn tại");
                }
                cat.setName(newName);
            }
        }
        if (fields.containsKey("parentId")) {
            Object v = fields.get("parentId");
            cat.setParentId(v != null ? ((Number) v).intValue() : null);
        }
        if (fields.containsKey("color")) {
            Object v = fields.get("color");
            cat.setColor(v != null ? v.toString() : null);
        }
        if (fields.containsKey("iconClass")) {
            Object v = fields.get("iconClass");
            cat.setIconClass(v != null ? v.toString() : null);
        }
        return categoryRepository.save(cat);
    }

    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy danh mục với ID: " + id);
        }
        categoryRepository.deleteAllBookLinks(id);
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public int getCategoryBookCount(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy danh mục với ID: " + id);
        }
        return categoryRepository.countBooksByCategoryId(id);
    }

    @Transactional(readOnly = true)
    public List<Books> getAlsoBorrowedBooks(Integer bookId, int limit) {
        List<Integer> bookIds = loanRepository.findAlsoBorrowedBookIds(bookId, PageRequest.of(0, limit));
        if (bookIds == null || bookIds.isEmpty()) {
            // Fallback to category-based similar books
            return getSimilarBooks(bookId, limit);
        }
        // Preserve order from query (already sorted by borrow frequency)
        List<Books> books = booksRepository.findAllById(bookIds);
        // Re-sort to match the frequency-ordered IDs from the query
        java.util.Map<Integer, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < bookIds.size(); i++) orderMap.put(bookIds.get(i), i);
        books.sort(java.util.Comparator.comparingInt(b -> orderMap.getOrDefault(b.getId(), Integer.MAX_VALUE)));
        return books;
    }

    public void migrateBooksToCategory(Integer fromId, Integer toId) {
        if (!categoryRepository.existsById(fromId))
            throw new NotFoundException("Không tìm thấy danh mục nguồn với ID: " + fromId);
        if (!categoryRepository.existsById(toId))
            throw new NotFoundException("Không tìm thấy danh mục đích với ID: " + toId);
        // Move books that are ONLY in fromId to toId (avoid duplicates)
        categoryRepository.migrateBooksToCategory(fromId, toId);
        // Delete remaining links (books that were in both)
        categoryRepository.deleteAllBookLinks(fromId);
    }
}