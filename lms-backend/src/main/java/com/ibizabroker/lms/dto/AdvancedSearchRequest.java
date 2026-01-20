package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🔍 Advanced Search Request DTO
 * 
 * Dùng cho endpoint POST /api/public/books/advanced-search
 * 
 * 📌 Search Filters:
 * - query: Text search (title, ISBN, description)
 * - authorIds: Filter by authors (OR logic)
 * - categoryIds: Filter by categories (OR logic)
 * - yearFrom/yearTo: Filter by publication year range
 * - availableOnly: Only show available books (in stock)
 * - sortBy: Sort results (name, year, popularity)
 * 
 * 👉 Pattern: Complex Query DTO
 * - All fields optional (partial filtering)
 * - Combine multiple filters with AND logic
 * - Within each filter (authors/categories), use OR logic
 * 
 * 🎯 Examples:
 * - Search "Java" + authorIds=[1,2] + yearFrom=2020 + sortBy="popularity"
 * - Search by categories only: categoryIds=[3,5] + availableOnly=true
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvancedSearchRequest {

    /**
     * Text search query (title, ISBN, description)
     */
    private String query;
    
    /**
     * Filter by author IDs (OR logic)
     */
    private List<Integer> authorIds;
    
    /**
     * Filter by category IDs (OR logic)
     */
    private List<Integer> categoryIds;
    
    /**
     * Publication year from (inclusive)
     */
    private Integer yearFrom;
    
    /**
     * Publication year to (inclusive)
     */
    private Integer yearTo;
    
    /**
     * Only show available books (availableQuantity > 0)
     */
    private Boolean availableOnly;
    
    /**
     * Sort by: "name", "year", "popularity"
     */
    private String sortBy;
}
