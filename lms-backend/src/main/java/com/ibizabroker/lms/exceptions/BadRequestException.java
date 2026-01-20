package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * ❌ Bad Request Exception (HTTP 400)
 * 
 * Ném khi request data không hợp lệ:
 * - Validation failed: Ngày trả < ngày mượn
 * - Business rule violated: Mượn quá 5 quyển cùng lúc
 * - Invalid format: ISBN format sai, email format sai
 * - Logic error: Trả sách chưa mượn
 * 
 * 🎯 Use Cases:
 * - `throw new BadRequestException("Return date must be after loan date", "LOAN_INVALID_DATE")`
 * - `throw new BadRequestException("Cannot borrow more than 5 books", "LOAN_LIMIT_EXCEEDED")`
 * - `throw new BadRequestException("Book already returned", "LOAN_ALREADY_RETURNED")`
 * 
 * 📝 Error Code Convention:
 * - LOAN_400: Generic loan validation error
 * - LOAN_INVALID_DATE: Specific date validation
 * - LOAN_LIMIT_EXCEEDED: Business rule violation
 * 
 * @author Library Management System
 * @since Phase 6: Exception Hierarchy
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor với message mặc định errorCode = "BAD_REQUEST"
     */
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructor với custom errorCode (recommended)
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi cụ thể (LOAN_INVALID_DATE, BOOK_NOT_AVAILABLE)
     */
    public BadRequestException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructor với root cause
     */
    public BadRequestException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.BAD_REQUEST, cause);
    }
}
