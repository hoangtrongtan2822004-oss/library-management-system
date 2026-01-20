package com.ibizabroker.lms.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * 🏗️ Base Exception - Abstract parent cho tất cả custom exceptions
 * 
 * 📌 Mục đích:
 * - DRY (Don't Repeat Yourself): Tránh duplicate code errorCode/httpStatus
 * - Chuẩn hóa: Tất cả exceptions đều có errorCode cho Frontend i18n
 * - Type Safety: Đảm bảo tất cả exceptions có cấu trúc nhất quán
 * 
 * 🎯 Architecture:
 * - errorCode: Mã lỗi nghiệp vụ (USER_001, BOOK_404, LOAN_CONFLICT)
 * - httpStatus: HTTP status code tương ứng (404, 400, 409, 403, 401)
 * - message: Thông báo lỗi chi tiết (for logging)
 * 
 * 📝 Usage:
 * ```java
 * public class NotFoundException extends BaseException {
 *     public NotFoundException(String message, String errorCode) {
 *         super(message, errorCode, HttpStatus.NOT_FOUND);
 *     }
 * }
 * 
 * // Frontend sẽ nhận:
 * // { "errorCode": "USER_404", "message": "User not found", "status": 404 }
 * ```
 * 
 * @author Library Management System
 * @since Phase 6: Entity Layer Upgrade (Exception Hierarchy)
 */
@Getter
public abstract class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 🏷️ Error Code cho Frontend i18n
     * 
     * Format: <RESOURCE>_<STATUS>
     * - USER_404: User không tồn tại
     * - BOOK_409: Sách bị trùng ISBN
     * - LOAN_400: Ngày trả < ngày mượn
     */
    private final String errorCode;

    /**
     * 📊 HTTP Status Code
     * 
     * - 400 Bad Request: Validation failed
     * - 401 Unauthorized: Token invalid/expired
     * - 403 Forbidden: Không có quyền
     * - 404 Not Found: Resource không tồn tại
     * - 409 Conflict: Trùng dữ liệu
     */
    private final int httpStatus;

    /**
     * Constructor với errorCode và httpStatus
     * 
     * @param message Chi tiết lỗi (for logging)
     * @param errorCode Mã lỗi nghiệp vụ (for Frontend)
     * @param httpStatus HTTP status code
     */
    protected BaseException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus.value();
    }

    /**
     * Constructor với errorCode, httpStatus, và root cause
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi nghiệp vụ
     * @param httpStatus HTTP status code
     * @param cause Exception gốc (for chaining)
     */
    protected BaseException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus.value();
    }
}
