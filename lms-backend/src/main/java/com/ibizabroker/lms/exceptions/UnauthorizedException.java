package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * 🔒 Unauthorized Exception (HTTP 401)
 * 
 * Ném khi authentication failed hoặc token không hợp lệ:
 * - Token expired: JWT hết hạn
 * - Token invalid: JWT signature sai, format sai
 * - Missing token: Request không có Authorization header
 * - Wrong credentials: Username/password sai
 * - Token revoked: Token bị thu hồi (logout, reset password)
 * 
 * 🎯 Use Cases:
 * - `throw new UnauthorizedException("Token expired", "TOKEN_EXPIRED")`
 * - `throw new UnauthorizedException("Invalid JWT signature", "TOKEN_INVALID")`
 * - `throw new UnauthorizedException("Missing Authorization header", "TOKEN_MISSING")`
 * - `throw new UnauthorizedException("Invalid credentials", "INVALID_CREDENTIALS")`
 * 
 * 📝 Error Code Convention:
 * - UNAUTHORIZED: Generic auth error
 * - TOKEN_EXPIRED: JWT hết hạn
 * - TOKEN_INVALID: JWT signature/format sai
 * - TOKEN_MISSING: Không có token
 * - INVALID_CREDENTIALS: Username/password sai
 * 
 * 🔐 Security Note:
 * Khác với ForbiddenException (403 - không có quyền), UnauthorizedException (401) nghĩa là:
 * - User chưa login hoặc token không hợp lệ
 * - Cần authenticate lại (re-login)
 * - Response should include WWW-Authenticate header
 * 
 * ⚡ Integration with JwtRequestFilter:
 * JwtRequestFilter sẽ catch ExpiredJwtException, SignatureException và ném UnauthorizedException
 * 
 * @author Library Management System
 * @since Phase 6: Exception Hierarchy
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor với message mặc định errorCode = "UNAUTHORIZED"
     */
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor với custom errorCode (recommended)
     * 
     * @param message Chi tiết lỗi
     * @param errorCode Mã lỗi cụ thể (TOKEN_EXPIRED, INVALID_CREDENTIALS)
     */
    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor với root cause (for JWT exceptions)
     */
    public UnauthorizedException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED, cause);
    }
}
