package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * 🔍 Not Found Exception (HTTP 404)
 * 
 * Ném khi resource không tồn tại trong database:
 * - Entity not found: User, Book, Loan, Category không tồn tại
 * - Nested resource: Book có ID nhưng Author không tồn tại
 * - Soft delete: Resource đã bị xóa (soft delete)
 * - Wrong ID: ID format hợp lệ nhưng không match record nào
 * 
 * 🎯 Use Cases:
 * - `throw new NotFoundException("User not found with ID: " + userId, "USER_NOT_FOUND")`
 * - `throw new NotFoundException("Book not found", "BOOK_NOT_FOUND")`
 * - `throw new NotFoundException("Loan not found with ID: " + loanId, "LOAN_NOT_FOUND")`
 * - `throw new NotFoundException("Category not found", "CATEGORY_NOT_FOUND")`
 * 
 * 📝 Error Code Convention:
 * - NOT_FOUND: Generic not found error
 * - USER_NOT_FOUND: User entity not found
 * - BOOK_NOT_FOUND: Book entity not found
 * - LOAN_NOT_FOUND: Loan entity not found
 * - AUTHOR_NOT_FOUND: Author entity not found
 * 
 * 🔍 Pattern:
 * ```java
 * User user = userRepository.findById(id)
 *     .orElseThrow(() -> new NotFoundException("User not found with ID: " + id, "USER_NOT_FOUND"));
 * ```
 * 
 * @author Library Management System
 * @since Phase 1 (Refactored in Phase 6: Exception Hierarchy)
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor với message mặc định errorCode = "NOT_FOUND"
     * 
     * @param message Chi tiết lỗi (e.g., "User not found with ID: 123")
     */
    public NotFoundException(String message) {
        super(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * Constructor với custom errorCode (recommended)
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi cụ thể (USER_NOT_FOUND, BOOK_NOT_FOUND)
     */
    public NotFoundException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.NOT_FOUND);
    }

    /**
     * Constructor với root cause
     */
    public NotFoundException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.NOT_FOUND, cause);
    }
}

