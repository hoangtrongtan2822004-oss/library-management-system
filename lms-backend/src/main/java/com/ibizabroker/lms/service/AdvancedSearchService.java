package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.entity.Books;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature 2: Advanced Search Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvancedSearchService {

    private final BooksRepository booksRepository;

    /**
     * Tìm kiếm nâng cao với nhiều tiêu chí
     */
    public Page<Books> advancedSearch(
            String query,
            List<Integer> authorIds,
            List<Integer> categoryIds,
            Integer yearFrom,
            Integer yearTo,
            Boolean availableOnly,
            String sortBy,
            Pageable pageable) {
        
        return booksRepository.advancedSearch(
                query, authorIds, categoryIds, yearFrom, yearTo, 
                availableOnly != null && availableOnly, pageable);
    }

    /**
     * Gợi ý sách tương tự (dựa trên thể loại và tác giả)
     */
    @Cacheable(value = "similar-books", key = "#bookId + '|' + #limit")
    @SuppressWarnings("null")
    public List<Books> getSimilarBooks(Integer bookId, int limit) {
        Books book = booksRepository.findById(bookId).orElse(null);
        if (book == null) return Collections.emptyList();

        List<Integer> categoryIds = book.getCategories().stream()
                .map(c -> c.getId())
                .collect(Collectors.toList());
        
        if (categoryIds.isEmpty()) return Collections.emptyList();

        return booksRepository.findSimilarBooks(bookId, categoryIds, PageRequest.of(0, limit));
    }

    /**
     * Autocomplete cho tìm kiếm
     */
    @Cacheable(value = "search-suggestions", key = "#query.toLowerCase() + '|' + #limit")
    public List<String> getSearchSuggestions(String query, int limit) {
        if (query == null || query.length() < 2) {
            return Collections.emptyList();
        }
        return booksRepository.findSearchSuggestions(query.toLowerCase(), PageRequest.of(0, limit));
    }

    /**
     * Gợi ý tác giả
     */
    @Cacheable(value = "author-suggestions", key = "#query.toLowerCase() + '|' + #limit")
    public List<String> getAuthorSuggestions(String query, int limit) {
        if (query == null || query.length() < 2) {
            return Collections.emptyList();
        }
        return booksRepository.findAuthorSuggestions(query.toLowerCase(), PageRequest.of(0, limit));
    }

    /**
     * Thống kê tìm kiếm phổ biến (có thể lưu vào Redis)
     */
    private final Map<String, Integer> searchStats = new LinkedHashMap<>();

    public void recordSearch(String query) {
        if (query != null && !query.trim().isEmpty()) {
            searchStats.merge(query.toLowerCase().trim(), 1, (a, b) -> a + b);
        }
    }

    public List<String> getPopularSearches(int limit) {
        return searchStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
