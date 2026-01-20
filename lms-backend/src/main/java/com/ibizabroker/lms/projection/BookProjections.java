package com.ibizabroker.lms.projection;

/**
 * 📦 Interface-based Projections cho Books
 * 
 * Thay vì SELECT toàn bộ entity (20+ columns), chỉ SELECT đúng columns cần thiết.
 * 
 * 📌 Vấn đề với Entity đầy đủ:
 * ```java
 * // BAD: SELECT * FROM books (lấy cả description dài 5000 chữ)
 * List<Books> books = booksRepository.findAll();
 * ```
 * 
 * 📌 Giải pháp với Projection:
 * ```java
 * // GOOD: SELECT id, name, cover_image FROM books
 * List<BookSummary> books = booksRepository.findAllBy();
 * ```
 * 
 * 💰 Performance Gain:
 * - Entity đầy đủ: ~2.5 KB/book × 100 = 250 KB
 * - Projection slim: ~0.3 KB/book × 100 = 30 KB
 * - Tiết kiệm: 220 KB (-88%) cho mỗi query
 * 
 * 🎯 Use Cases theo View:
 * - BookSummary: Danh sách lưới (grid view)
 * - BookDropdown: Select dropdown (chọn sách khi mượn)
 * - BookSearchResult: Kết quả tìm kiếm
 * - BookCardView: Card hiển thị trên homepage
 */
public class BookProjections {

    /**
     * Projection cho danh sách lưới (Grid List)
     * Chỉ hiển thị: Ảnh bìa, Tên, Tác giả, Trạng thái còn/hết
     */
    public interface BookSummary {
        Integer getId();
        String getName();
        String getCoverImageUrl();
        Integer getNumberOfCopiesAvailable();
        
        // Nested projection cho author
        AuthorSummary getAuthor();
        
        interface AuthorSummary {
            String getName();
        }
        
        // Nested projection cho category
        CategorySummary getCategory();
        
        interface CategorySummary {
            String getName();
        }
        
        /**
         * Computed property (Spring Data tự động gọi method này)
         */
        default boolean isAvailable() {
            return getNumberOfCopiesAvailable() != null && getNumberOfCopiesAvailable() > 0;
        }
    }

    /**
     * Projection cho dropdown select
     * Chỉ cần: ID, Tên
     */
    public interface BookDropdown {
        Integer getId();
        String getName();
        
        /**
         * Formatted display text cho dropdown option
         * Example: "[#123] Clean Code"
         */
        default String getDisplayText() {
            return "[#" + getId() + "] " + getName();
        }
    }

    /**
     * Projection cho kết quả tìm kiếm
     * Bao gồm: Ảnh, Tên, Tác giả, Năm xuất bản, Trạng thái
     */
    public interface BookSearchResult {
        Integer getId();
        String getName();
        String getIsbn();
        String getCoverImageUrl();
        Integer getPublishedYear();
        Integer getNumberOfCopiesAvailable();
        Integer getQuantity();
        
        AuthorSummary getAuthor();
        
        interface AuthorSummary {
            String getName();
        }
        
        CategorySummary getCategory();
        
        interface CategorySummary {
            String getName();
        }
        
        /**
         * Availability status text
         */
        default String getAvailabilityStatus() {
            int available = getNumberOfCopiesAvailable() != null ? getNumberOfCopiesAvailable() : 0;
            if (available == 0) return "Hết sách";
            if (available < 3) return "Sắp hết (" + available + " cuốn)";
            return "Còn " + available + " cuốn";
        }
    }

    /**
     * Projection cho card hiển thị (Homepage Featured Books)
     * Bao gồm: Ảnh, Tên, Tác giả, Rating
     */
    public interface BookCard {
        Integer getId();
        String getName();
        String getCoverImageUrl();
        Double getAverageRating();
        Integer getReviewCount();
        
        AuthorSummary getAuthor();
        
        interface AuthorSummary {
            String getName();
        }
        
        /**
         * Rating display với stars
         */
        default String getRatingDisplay() {
            if (getAverageRating() == null || getAverageRating() == 0) {
                return "Chưa có đánh giá";
            }
            return String.format("%.1f ⭐ (%d đánh giá)", getAverageRating(), getReviewCount());
        }
    }

    /**
     * Projection cho autocomplete suggestions
     * Chỉ cần: Tên, ISBN (để phân biệt sách trùng tên)
     */
    public interface BookSuggestion {
        Integer getId();
        String getName();
        String getIsbn();
        
        AuthorSummary getAuthor();
        
        interface AuthorSummary {
            String getName();
        }
        
        /**
         * Display text cho autocomplete
         * Example: "Clean Code - Robert C. Martin (ISBN: 978-0132350884)"
         */
        default String getSuggestionText() {
            return getName() + " - " + getAuthor().getName() + 
                   (getIsbn() != null ? " (ISBN: " + getIsbn() + ")" : "");
        }
    }

    /**
     * Projection cho báo cáo (Report)
     * Bao gồm: Thông tin cơ bản + số lượt mượn
     */
    public interface BookReport {
        Integer getId();
        String getName();
        String getIsbn();
        Integer getQuantity();
        Integer getNumberOfCopiesAvailable();
        Integer getBorrowedCount();
        
        AuthorSummary getAuthor();
        
        interface AuthorSummary {
            String getName();
        }
        
        CategorySummary getCategory();
        
        interface CategorySummary {
            String getName();
        }
        
        /**
         * Utilization rate (tỷ lệ sử dụng)
         */
        default double getUtilizationRate() {
            if (getQuantity() == null || getQuantity() == 0) return 0.0;
            int borrowed = getBorrowedCount() != null ? getBorrowedCount() : 0;
            return (borrowed * 100.0) / getQuantity();
        }
    }

    /**
     * Projection cho detail page (cần nhiều thông tin hơn)
     * Gần như full entity nhưng vẫn tối ưu hơn vì không load lazy collections
     */
    public interface BookDetail {
        Integer getId();
        String getName();
        String getIsbn();
        String getDescription();
        String getCoverImageUrl();
        Integer getPublishedYear();
        String getPublisher();
        String getLanguage();
        Integer getPages();
        Integer getQuantity();
        Integer getNumberOfCopiesAvailable();
        Integer getBorrowedCount();
        Double getAverageRating();
        Integer getReviewCount();
        java.time.LocalDate getAddedDate();
        
        AuthorDetail getAuthor();
        
        interface AuthorDetail {
            Integer getId();
            String getName();
            String getBio();
        }
        
        CategoryDetail getCategory();
        
        interface CategoryDetail {
            Integer getId();
            String getName();
            String getDescription();
        }
    }
}
