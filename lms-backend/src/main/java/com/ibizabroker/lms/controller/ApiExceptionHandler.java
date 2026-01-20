package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.exceptions.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🛡️ Centralized Exception Handler
 * 
 * Xử lý tất cả exceptions trong hệ thống và trả về ApiResponse chuẩn:
 * - BaseException: Custom exceptions với errorCode
 * - ValidationException: Multi-field validation errors
 * - MethodArgumentNotValidException: Jakarta Bean Validation
 * - OptimisticLockException: Concurrent update conflicts
 * - Generic Exception: Catch-all handler
 * 
 * 📌 Response Format:
 * ```json
 * {
 *   "success": false,
 *   "message": "User not found with ID: 123",
 *   "errorCode": "USER_NOT_FOUND",
 *   "data": null,
 *   "traceId": "abc-123-xyz"
 * }
 * ```
 * 
 * @author Library Management System
 * @since Phase 3 (Refactored in Phase 6: Exception Hierarchy)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 🏗️ Handle BaseException - All custom exceptions
     * 
     * Catches: NotFoundException, BadRequestException, ConflictException,
     *          ForbiddenException, UnauthorizedException, ValidationException
     * 
     * Returns: ApiResponse với errorCode cho Frontend i18n
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Object>> handleBaseException(BaseException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())  // 🎯 Key feature: errorCode cho i18n
                .data(null)
                .build();
        
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(response);
    }

    /**
     * ✅ Handle ValidationException - Multi-field validation errors
     * 
     * Special case: ValidationException có fieldErrors map
     * Returns: ApiResponse với fieldErrors trong data
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(ValidationException ex) {
        Map<String, Object> errorData = new HashMap<>();
        
        if (ex.hasFieldErrors()) {
            errorData.put("fieldErrors", ex.getFieldErrors());
        }
        
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .data(errorData)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 📋 Handle Jakarta Bean Validation (@Valid, @NotEmpty, etc.)
     * 
     * Catches: MethodArgumentNotValidException from @Valid
     * Returns: ApiResponse với fieldErrors trong data
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing  // Keep first error if duplicate field
                ));
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("fieldErrors", fieldErrors);
        
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .data(errorData)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 🔒 Handle OptimisticLockException - Concurrent update conflict
     * 
     * Occurs when: 2 users update same entity (Books, Loan, etc.)
     * Solution: Frontend should reload data and retry
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLockException(OptimisticLockException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Data was modified by another user. Please reload and try again.")
                .errorCode("OPTIMISTIC_LOCK_ERROR")
                .data(null)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * ⚠️ Handle IllegalStateException - Business rule violation
     * 
     * Legacy exception: Still used in some services
     * Consider: Migrate to BadRequestException with errorCode
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("ILLEGAL_STATE")
                .data(null)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 💥 Handle Generic Exception - Catch-all handler
     * 
     * Catches: Unexpected runtime errors, NPE, SQLException, etc.
     * Returns: Generic error message (don't expose internal details)
     * 
     * 🔍 Logging: Prints stacktrace for debugging (consider using proper logger)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        // TODO: Replace printStackTrace with proper logging (Log4j, SLF4J)
        ex.printStackTrace();
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("An unexpected error occurred. Please try again later.")
                .errorCode("INTERNAL_SERVER_ERROR")
                .data(null)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
