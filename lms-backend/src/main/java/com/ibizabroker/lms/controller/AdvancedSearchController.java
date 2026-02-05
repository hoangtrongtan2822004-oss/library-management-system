package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.AdvancedSearchRequest;
import com.ibizabroker.lms.dto.BookListDto;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.service.AdvancedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feature 2: Advanced Search Controller
 */
@RestController
@RequestMapping("/api/public/search")
@RequiredArgsConstructor
public class AdvancedSearchController {

    private final AdvancedSearchService searchService;

    @PostMapping("/advanced")
    public ResponseEntity<Page<BookListDto>> advancedSearch(
            @RequestBody AdvancedSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Record search for analytics
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            searchService.recordSearch(request.getQuery());
        }
        
        Page<Books> results = searchService.advancedSearch(
                request.getQuery(),
                request.getAuthorIds(),
                request.getCategoryIds(),
                request.getYearFrom(),
                request.getYearTo(),
                request.getAvailableOnly(),
                request.getSortBy(),
                pageable);

        Page<BookListDto> dtoPage = results.map(BookListDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/similar/{bookId}")
    public ResponseEntity<List<BookListDto>> getSimilarBooks(
            @PathVariable Integer bookId,
            @RequestParam(defaultValue = "5") int limit) {
        List<Books> books = searchService.getSimilarBooks(bookId, limit);
        return ResponseEntity.ok(
            books.stream().map(BookListDto::fromEntity).toList()
        );
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.getSearchSuggestions(query, limit));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularSearches(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.getPopularSearches(limit));
    }
}
