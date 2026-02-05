package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Books;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 📚 Books Repository
 * * ✅ CRUD methods (JpaRepository)
 * ✅ Dynamic queries (JpaSpecificationExecutor)
 * ✅ MySQL Full-Text Search (Native Query)
 * ✅ EntityGraph (Prevent N+1)
 */
public interface BooksRepository extends JpaRepository<Books, Integer>, JpaSpecificationExecutor<Books> {

    // ====================================================================
    // 📦 STOCK MANAGEMENT (ATOMIC UPDATES)
    // ====================================================================

    @Transactional
    @Modifying
    @Query("UPDATE Books b SET b.numberOfCopiesAvailable = b.numberOfCopiesAvailable - 1 " +
           "WHERE b.id = :bookId AND b.numberOfCopiesAvailable > 0")
    int decrementAvailable(@Param("bookId") Integer bookId);

    @Transactional
    @Modifying
    @Query("UPDATE Books b SET b.numberOfCopiesAvailable = b.numberOfCopiesAvailable + 1 " +
           "WHERE b.id = :bookId")
    int incrementAvailable(@Param("bookId") Integer bookId);

    // ====================================================================
    // 🔒 PESSIMISTIC LOCKING
    // ====================================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Books b WHERE b.id = :id")
    Optional<Books> findByIdWithLock(@Param("id") Integer id);
    
    List<Books> findByNumberOfCopiesAvailableGreaterThan(int n);

    // ====================================================================
    // 🚀 FETCHING & PAGINATION (Fix N+1)
    // ====================================================================

    @Override
    @EntityGraph(attributePaths = {"authors", "categories"})
    List<Books> findAll();

    @Override
    @EntityGraph(attributePaths = {"authors", "categories"})
    Page<Books> findAll(Pageable pageable);

       @Override
       @EntityGraph(attributePaths = {"authors", "categories"})
       @Query("SELECT b FROM Books b LEFT JOIN FETCH b.authors LEFT JOIN FETCH b.categories WHERE b.id = :id")
       Optional<Books> findById(@Param("id") Integer bookId);

       @EntityGraph(attributePaths = {"authors", "categories"})
       @Query("SELECT b FROM Books b LEFT JOIN FETCH b.authors LEFT JOIN FETCH b.categories WHERE b.id IN :ids")
       List<Books> findAllByIdIn(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Books b ORDER BY b.id DESC")
    List<Books> findNewestBooks(Pageable pageable);

    // ====================================================================
    // 🔍 JPQL SEARCH (Filters)
    // ====================================================================

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query(value = "SELECT DISTINCT b FROM Books b " +
                   "LEFT JOIN b.authors a " +
                   "LEFT JOIN b.categories c " +
                   "WHERE " +
                   "(:search IS NULL OR :search = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                   "(:genre IS NULL OR :genre = '' OR c.name = :genre) AND " +
                   "(:availableOnly = false OR b.numberOfCopiesAvailable > 0)",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Books b " +
                        "LEFT JOIN b.authors a " +
                        "LEFT JOIN b.categories c " +
                        "WHERE " +
                        "(:search IS NULL OR :search = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "(:genre IS NULL OR :genre = '' OR c.name = :genre) AND " +
                        "(:availableOnly = false OR b.numberOfCopiesAvailable > 0)")
    Page<Books> findWithFiltersAndPagination(
        @Param("search") String search,
        @Param("genre") String genre,
        @Param("availableOnly") boolean availableOnly,
        Pageable pageable);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query(value = "SELECT DISTINCT b FROM Books b " +
                   "LEFT JOIN b.authors a " +
                   "LEFT JOIN b.categories c " +
                   "WHERE (:query IS NULL OR :query = '' OR " +
                   "       LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                   "       LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                   "       LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "AND (:authorIds IS NULL OR a.id IN :authorIds) " +
                   "AND (:categoryIds IS NULL OR c.id IN :categoryIds) " +
                   "AND (:yearFrom IS NULL OR b.publishedYear >= :yearFrom) " +
                   "AND (:yearTo IS NULL OR b.publishedYear <= :yearTo) " +
                   "AND (:availableOnly = false OR b.numberOfCopiesAvailable > 0)",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Books b " +
                        "LEFT JOIN b.authors a " +
                        "LEFT JOIN b.categories c " +
                        "WHERE (:query IS NULL OR :query = '' OR " +
                        "       LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "       LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "       LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND (:authorIds IS NULL OR a.id IN :authorIds) " +
                        "AND (:categoryIds IS NULL OR c.id IN :categoryIds) " +
                        "AND (:yearFrom IS NULL OR b.publishedYear >= :yearFrom) " +
                        "AND (:yearTo IS NULL OR b.publishedYear <= :yearTo) " +
                        "AND (:availableOnly = false OR b.numberOfCopiesAvailable > 0)")
    Page<Books> advancedSearch(
        @Param("query") String query,
        @Param("authorIds") List<Integer> authorIds,
        @Param("categoryIds") List<Integer> categoryIds,
        @Param("yearFrom") Integer yearFrom,
        @Param("yearTo") Integer yearTo,
        @Param("availableOnly") boolean availableOnly,
        Pageable pageable);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Books b " +
           "JOIN b.categories c " +
           "WHERE b.id <> :excludeBookId " +
           "AND c.id IN :categoryIds " +
           "ORDER BY b.numberOfCopiesAvailable DESC")
    List<Books> findSimilarBooks(
        @Param("excludeBookId") Integer excludeBookId,
        @Param("categoryIds") List<Integer> categoryIds,
        Pageable pageable);

    // ====================================================================
    // 💡 SUGGESTIONS
    // ====================================================================

    @Query("SELECT DISTINCT b.name FROM Books b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findSearchSuggestions(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT a.name FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findAuthorSuggestions(@Param("query") String query, Pageable pageable);

    Optional<Books> findByIsbnIgnoreCase(String isbn);

    // ====================================================================
    // 🚀 MYSQL NATIVE FULL-TEXT SEARCH
    // ====================================================================

    /**
     * Note: VS Code or IDEs may flag "SQL_SYNTAX" errors here because 
     * they don't support MySQL specific 'MATCH' syntax validation.
     * Use @SuppressWarnings to ignore in some IDEs.
     */

    // 1. Natural Language Search
    @Query(value = "SELECT * FROM books WHERE MATCH(name, isbn) AGAINST(:query IN NATURAL LANGUAGE MODE)", 
           countQuery = "SELECT COUNT(*) FROM books WHERE MATCH(name, isbn) AGAINST(:query IN NATURAL LANGUAGE MODE)",
           nativeQuery = true)
    Page<Books> fullTextSearch(@Param("query") String query, Pageable pageable);

    // 2. Boolean Mode Search
    @Query(value = "SELECT * FROM books WHERE MATCH(name, isbn) AGAINST(:query IN BOOLEAN MODE)",
           countQuery = "SELECT COUNT(*) FROM books WHERE MATCH(name, isbn) AGAINST(:query IN BOOLEAN MODE)",
           nativeQuery = true)
    Page<Books> fullTextSearchBoolean(@Param("query") String query, Pageable pageable);

    // 3. Description Search
    @Query(value = "SELECT * FROM books WHERE MATCH(description) AGAINST(:query IN NATURAL LANGUAGE MODE)",
           countQuery = "SELECT COUNT(*) FROM books WHERE MATCH(description) AGAINST(:query IN NATURAL LANGUAGE MODE)",
           nativeQuery = true)
    Page<Books> fullTextSearchDescription(@Param("query") String query, Pageable pageable);

    // 4. Autocomplete Suggestions (Native)
    @Query(value = "SELECT DISTINCT name FROM books WHERE MATCH(name, isbn) AGAINST(CONCAT(:query, '*') IN BOOLEAN MODE) LIMIT 10",
           nativeQuery = true)
    List<String> fullTextSearchSuggestions(@Param("query") String query);
}