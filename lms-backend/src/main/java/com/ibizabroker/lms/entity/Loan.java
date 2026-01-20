package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 📖 Loan Entity
 * 
 * Entity quản lý thông tin mượn sách trong thư viện
 * 
 * 🎯 Enhancements:
 * - ✅ Lombok: @Getter @Setter thay manual getters/setters
 * - ✅ BaseEntity: Tự động audit (createdAt, updatedAt, createdBy, updatedBy)
 * - ✅ JPA Relationships: @ManyToOne với Books và Users (thay Integer bookId/memberId)
 * - ✅ Type-safe Enum: FineStatus thay String fineStatus (tránh lỗi chính tả)
 * 
 * ⚠️ Lazy Loading:
 * - @ManyToOne(fetch = LAZY) để tránh N+1 query
 * - Access: loan.getBook().getName() → tự động JOIN
 * 
 * 📌 ToString exclude "book" và "member" để tránh circular reference
 */
@Getter
@Setter
@ToString(exclude = {"book", "member"})
@Entity
@Table(name = "loans")
public class Loan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 📚 Book being loaned
     * 
     * Dùng @ManyToOne thay Integer bookId để:
     * - Truy cập trực tiếp: loan.getBook().getName()
     * - Lazy loading: Chỉ query khi cần
     * - Type-safe: Compile-time check
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Books book;

    /**
     * 👤 Member borrowing the book
     * 
     * Dùng @ManyToOne thay Integer memberId để:
     * - Truy cập trực tiếp: loan.getMember().getFullName()
     * - Lazy loading: Chỉ query khi cần
     * - Type-safe: Compile-time check
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Users member;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal fineAmount;

    /**
     * 💰 Fine payment status
     * 
     * Dùng Enum thay String để:
     * - Type-safe: Không thể gán "paid" (lowercase) nhầm
     * - Auto-complete: IDE gợi ý FineStatus.PAID, UNPAID, WAIVED, PARTIALLY_PAID
     * - Refactor-safe: Đổi tên enum → IDE update toàn bộ
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_status", length = 20)
    private FineStatus fineStatus;

    // ===== Compatibility Methods (for old code using bookId/memberId) =====
    
    /**
     * Backward compatibility: getBookId()
     * 
     * ⚠️ Deprecated: Dùng getBook().getId() thay vì getBookId()
     */
    public Integer getBookId() {
        return book != null ? book.getId() : null;
    }

    /**
     * Backward compatibility: getMemberId()
     * 
     * ⚠️ Deprecated: Dùng getMember().getUserId() thay vì getMemberId()
     */
    public Integer getMemberId() {
        return member != null ? member.getUserId() : null;  // ✅ Use getUserId() not getId()
    }
}