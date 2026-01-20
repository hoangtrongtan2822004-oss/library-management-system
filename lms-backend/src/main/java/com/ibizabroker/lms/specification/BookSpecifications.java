package com.ibizabroker.lms.specification;

import com.ibizabroker.lms.entity.Author;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Category;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔍 JPA Specifications cho Dynamic Query Builder
 * 
 * Thay thế chuỗi @Query dài dòng bằng code Java type-safe và dễ bảo trì.
 * 
 * 📌 Vấn đề với @Query cũ:
 * ```java
 * @Query("SELECT b FROM Books b WHERE " +
 *        "(:search IS NULL OR LOWER(b.name) LIKE ...) AND " +
 *        "(:genre IS NULL OR c.name = :genre) AND ...")
 * ```
 * - Chuỗi SQL dài 20+ dòng, dễ lỗi cú pháp
 * - Khó test, khó debug
 * - Thêm điều kiện mới phải sửa cả chuỗi String
 * 
 * 📌 Giải pháp với Specification:
 * ```java
 * Specification<Books> spec = BookSpecifications.builder()
 *     .withSearch("Java")
 *     .withCategories(List.of(1, 2, 3))
 *     .onlyAvailable(true)
 *     .build();
 * Page<Books> result = booksRepository.findAll(spec, pageable);
 * ```
 * 
 * 💡 Lợi ích:
 * - Type-safe: IDE check compile-time
 * - Dễ test: Có thể test từng điều kiện riêng lẻ
 * - Dễ mở rộng: Thêm điều kiện mới chỉ cần thêm 1 method
 * - Reusable: Kết hợp Specification với nhau bằng and(), or()
 * 
 * 🎯 Use Cases:
 * - Advanced search form với 10+ filters
 * - Admin dashboard với dynamic report filters
 * - Public search với optional filters (genre, year, author...)
 */
public class BookSpecifications {

    /**
     * Tìm kiếm theo text trong tên sách, ISBN, hoặc tên tác giả
     */
    public static Specification<Books> hasSearchText(String searchText) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(searchText)) {
                return cb.conjunction(); // WHERE 1=1 (always true)
            }
            
            String pattern = "%" + searchText.toLowerCase() + "%";
            
            // JOIN với authors để search theo tên tác giả
            Join<Books, Author> authorsJoin = root.join("authors", JoinType.LEFT);
            
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("isbn")), pattern),
                cb.like(cb.lower(authorsJoin.get("name")), pattern)
            );
        };
    }

    /**
     * Lọc theo danh sách tác giả (ID)
     */
    public static Specification<Books> hasAuthors(List<Integer> authorIds) {
        return (root, query, cb) -> {
            if (authorIds == null || authorIds.isEmpty()) {
                return cb.conjunction();
            }
            
            Join<Books, Author> authorsJoin = root.join("authors", JoinType.INNER);
            return authorsJoin.get("id").in(authorIds);
        };
    }

    /**
     * Lọc theo danh sách thể loại (ID)
     */
    public static Specification<Books> hasCategories(List<Integer> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return cb.conjunction();
            }
            
            Join<Books, Category> categoriesJoin = root.join("categories", JoinType.INNER);
            return categoriesJoin.get("id").in(categoryIds);
        };
    }

    /**
     * Lọc theo tên thể loại (String)
     */
    public static Specification<Books> hasCategoryName(String categoryName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(categoryName)) {
                return cb.conjunction();
            }
            
            Join<Books, Category> categoriesJoin = root.join("categories", JoinType.INNER);
            return cb.equal(categoriesJoin.get("name"), categoryName);
        };
    }

    /**
     * Lọc theo khoảng năm xuất bản
     */
    public static Specification<Books> publishedBetween(Integer yearFrom, Integer yearTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedYear"), yearFrom));
            }
            
            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedYear"), yearTo));
            }
            
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Chỉ lấy sách còn hàng
     */
    public static Specification<Books> isAvailable() {
        return (root, query, cb) -> cb.greaterThan(root.get("numberOfCopiesAvailable"), 0);
    }

    /**
     * Lọc theo ISBN
     */
    public static Specification<Books> hasIsbn(String isbn) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(isbn)) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("isbn")), isbn.toLowerCase());
        };
    }

    /**
     * Lọc sách được thêm sau ngày X
     */
    public static Specification<Books> addedAfter(java.time.LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("addedDate"), date);
        };
    }

    /**
     * Builder Pattern để ghép nhiều điều kiện lại
     * 
     * Example:
     * ```java
     * Specification<Books> spec = BookSpecifications.builder()
     *     .withSearch("Java Programming")
     *     .withCategories(List.of(1, 2))
     *     .withYearRange(2020, 2024)
     *     .onlyAvailable(true)
     *     .build();
     * 
     * Page<Books> results = booksRepository.findAll(spec, PageRequest.of(0, 10));
     * ```
     */
    public static SpecificationBuilder builder() {
        return new SpecificationBuilder();
    }

    public static class SpecificationBuilder {
        private final List<Specification<Books>> specs = new ArrayList<>();

        public SpecificationBuilder withSearch(String searchText) {
            if (StringUtils.hasText(searchText)) {
                specs.add(hasSearchText(searchText));
            }
            return this;
        }

        public SpecificationBuilder withAuthors(List<Integer> authorIds) {
            if (authorIds != null && !authorIds.isEmpty()) {
                specs.add(hasAuthors(authorIds));
            }
            return this;
        }

        public SpecificationBuilder withCategories(List<Integer> categoryIds) {
            if (categoryIds != null && !categoryIds.isEmpty()) {
                specs.add(hasCategories(categoryIds));
            }
            return this;
        }

        public SpecificationBuilder withCategoryName(String categoryName) {
            if (StringUtils.hasText(categoryName)) {
                specs.add(hasCategoryName(categoryName));
            }
            return this;
        }

        public SpecificationBuilder withYearRange(Integer yearFrom, Integer yearTo) {
            if (yearFrom != null || yearTo != null) {
                specs.add(publishedBetween(yearFrom, yearTo));
            }
            return this;
        }

        public SpecificationBuilder onlyAvailable(Boolean availableOnly) {
            if (Boolean.TRUE.equals(availableOnly)) {
                specs.add(isAvailable());
            }
            return this;
        }

        public SpecificationBuilder withIsbn(String isbn) {
            if (StringUtils.hasText(isbn)) {
                specs.add(hasIsbn(isbn));
            }
            return this;
        }

        public Specification<Books> build() {
            if (specs.isEmpty()) {
                return (root, query, cb) -> cb.conjunction(); // No filters
            }
            
            // Combine all specs with AND
            Specification<Books> result = specs.get(0);
            for (int i = 1; i < specs.size(); i++) {
                result = result.and(specs.get(i));
            }
            
            return result;
        }
    }
}
