package com.ibizabroker.lms.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.PrePersist;

/**
 * 📚 Books Entity
 * 
 * Entity quản lý thông tin sách trong thư viện
 * 
 * 🎯 Enhancements:
 * - ✅ Lombok: @Getter @Setter thay manual getters/setters
 * - ✅ BaseEntity: Tự động audit (createdAt, updatedAt, createdBy, updatedBy)
 * - ✅ Optimistic Locking: @Version để tránh conflict khi update numberOfCopiesAvailable
 * - ✅ Lazy Loading: @ManyToMany với BatchSize để tránh N+1 query
 * 
 * ⚠️ Optimistic Locking:
 * - Mỗi lần update, Hibernate check version
 * - Nếu version khác → OptimisticLockException
 * - Dùng cho trường nhạy cảm: numberOfCopiesAvailable
 */
@Getter
@Setter
@Entity
@Table(name = "books")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Books extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonAlias({"bookName"})
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @JoinTable(
        name = "book_categories",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(name = "number_of_copies_available")
    @JsonAlias({"noOfCopies", "numberOfCopies"})
    private Integer numberOfCopiesAvailable;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(length = 32)
    private String isbn;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    /**
     * Ngày sách được thêm vào thư viện (tự động gán khi persist nếu null)
     */
    @Column(name = "added_date")
    private LocalDate addedDate;

    /**
     * 🔒 Version field for Optimistic Locking
     * 
     * Hibernate tự động tăng version mỗi lần update.
     * Nếu 2 user cùng update 1 record → user sau sẽ gặp OptimisticLockException
     * 
     * Use case: Tránh 2 thủ thư cùng lúc "Mượn sách" → numberOfCopiesAvailable bị sai
     */
    @Version
    @Column(name = "version")
    private Integer version;

    // ==== ALIAS Methods (Compatibility with old code) ====
    @JsonIgnore
    public String getBookName() { return getName(); }
    public void setBookName(String v) { setName(v); }

    @JsonIgnore
    public Integer getNoOfCopies() { return getNumberOfCopiesAvailable(); }
    public void setNoOfCopies(Integer v) { setNumberOfCopiesAvailable(v); }

    // ===== Business Logic Methods =====
    
    /**
     * 📤 Mượn sách - Giảm số lượng
     * 
     * @throws IllegalStateException nếu hết sách
     */
    public void borrowBook() {
        if (numberOfCopiesAvailable == null) numberOfCopiesAvailable = 0;
        if (numberOfCopiesAvailable <= 0) {
            throw new IllegalStateException("No copies available for book: " + name);
        }
        numberOfCopiesAvailable--;
    }

    /**
     * 📥 Trả sách - Tăng số lượng
     */
    public void returnBook() {
        if (numberOfCopiesAvailable == null) numberOfCopiesAvailable = 0;
        numberOfCopiesAvailable++;
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.addedDate == null) this.addedDate = LocalDate.now();
    }
    
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    private List<Review> reviews = new ArrayList<>();
}