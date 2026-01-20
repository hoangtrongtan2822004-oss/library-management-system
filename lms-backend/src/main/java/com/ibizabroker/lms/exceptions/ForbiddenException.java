package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * 🚫 Forbidden Exception (HTTP 403)
 * 
 * Ném khi user đã authenticated nhưng không có quyền thực hiện action:
 * - Role mismatch: User thường cố xóa sách (requires ADMIN)
 * - Resource ownership: User cố xóa review của người khác
 * - Business permission: User bị banned cố mượn sách
 * - Feature access: User cố truy cập tính năng premium
 * 
 * 🎯 Use Cases:
 * - `throw new ForbiddenException("Only ADMIN can delete books", "ADMIN_REQUIRED")`
 * - `throw new ForbiddenException("Cannot delete other user's review", "REVIEW_NOT_OWNED")`
 * - `throw new ForbiddenException("Account suspended, cannot borrow", "ACCOUNT_SUSPENDED")`
 * - `throw new ForbiddenException("Premium feature only", "PREMIUM_REQUIRED")`
 * 
 * 📝 Error Code Convention:
 * - FORBIDDEN: Generic forbidden error
 * - ADMIN_REQUIRED: Requires ADMIN role
 * - RESOURCE_NOT_OWNED: User không sở hữu resource
 * - ACCOUNT_SUSPENDED: Tài khoản bị khóa
 * 
 * 🔐 Security Note:
 * Khác với UnauthorizedException (401 - chưa login), ForbiddenException (403) nghĩa là:
 * - User đã login thành công
 * - JWT token hợp lệ
 * - Nhưng user không có quyền thực hiện action cụ thể
 * 
 * @author Library Management System
 * @since Phase 6: Exception Hierarchy
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor với message mặc định errorCode = "FORBIDDEN"
     */
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    /**
     * Constructor với custom errorCode (recommended)
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi cụ thể (ADMIN_REQUIRED, RESOURCE_NOT_OWNED)
     */
    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructor với root cause
     */
    public ForbiddenException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.FORBIDDEN, cause);
    }
}
