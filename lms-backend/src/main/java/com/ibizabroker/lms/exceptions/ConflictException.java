package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * ⚠️ Conflict Exception (HTTP 409)
 * 
 * Ném khi resource đã tồn tại hoặc xung đột dữ liệu:
 * - Duplicate key: Email đã tồn tại, ISBN đã tồn tại
 * - Concurrent modification: 2 users cùng sửa 1 record
 * - Business conflict: Sách đang được mượn, không thể xóa
 * - State conflict: Loan đã được trả, không thể trả lại
 * 
 * 🎯 Use Cases:
 * - `throw new ConflictException("Email already exists", "USER_EMAIL_EXISTS")`
 * - `throw new ConflictException("ISBN already exists", "BOOK_ISBN_EXISTS")`
 * - `throw new ConflictException("Book is currently on loan", "BOOK_ON_LOAN")`
 * - `throw new ConflictException("Loan already returned", "LOAN_ALREADY_RETURNED")`
 * 
 * 📝 Error Code Convention:
 * - USER_409: Generic user conflict
 * - USER_EMAIL_EXISTS: Duplicate email
 * - BOOK_ISBN_EXISTS: Duplicate ISBN
 * - LOAN_CONFLICT: Generic loan conflict
 * 
 * ⚡ Optimistic Locking:
 * When OptimisticLockException occurs, wrap it:
 * ```java
 * catch (OptimisticLockException e) {
 *     throw new ConflictException("Data was modified by another user", "OPTIMISTIC_LOCK_ERROR", e);
 * }
 * ```
 * 
 * @author Library Management System
 * @since Phase 6: Exception Hierarchy
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class ConflictException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor với message mặc định errorCode = "CONFLICT"
     */
    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    /**
     * Constructor với custom errorCode (recommended)
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi cụ thể (USER_EMAIL_EXISTS, BOOK_ISBN_EXISTS)
     */
    public ConflictException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.CONFLICT);
    }

    /**
     * Constructor với root cause (for OptimisticLockException)
     */
    public ConflictException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.CONFLICT, cause);
    }
}
