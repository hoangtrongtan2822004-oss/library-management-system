package com.ibizabroker.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ Validation Exception (HTTP 400)
 * 
 * Ném khi custom validation logic failed (ngoài Jakarta @Valid):
 * - Complex validation: ISBN checksum failed, date range invalid
 * - Cross-field validation: startDate > endDate
 * - Business validation: Cannot borrow book (already borrowed 5 books)
 * - Multiple field errors: Return errors map cho nhiều fields
 * 
 * 🎯 Use Cases:
 * - `throw new ValidationException("Invalid ISBN checksum", "ISBN_INVALID")`
 * - `throw new ValidationException("Start date must be before end date", "DATE_RANGE_INVALID")`
 * - Multi-field validation:
 * ```java
 * Map<String, String> errors = new HashMap<>();
 * errors.put("email", "Email already exists");
 * errors.put("phone", "Phone format invalid");
 * throw new ValidationException("Validation failed", "VALIDATION_ERROR", errors);
 * ```
 * 
 * 📝 Error Code Convention:
 * - VALIDATION_ERROR: Generic validation error
 * - ISBN_INVALID: ISBN checksum failed
 * - DATE_RANGE_INVALID: Date range validation
 * - FIELD_REQUIRED: Required field missing
 * 
 * 🔍 vs BadRequestException:
 * - ValidationException: Input format/validation issues (field-level errors)
 * - BadRequestException: Business logic violations (action-level errors)
 * 
 * @author Library Management System
 * @since Phase 6: Exception Hierarchy
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ValidationException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 📋 Field-level validation errors
     * 
     * Format: { "email": "Invalid format", "age": "Must be >= 18" }
     * Frontend can display errors next to corresponding fields
     */
    private final Map<String, String> fieldErrors;

    /**
     * Constructor với single message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructor với custom errorCode
     */
    public ValidationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructor với multiple field errors (recommended for forms)
     * 
     * @param message General error message
     * @param errorCode Error code
     * @param fieldErrors Map of field → error message
     */
    public ValidationException(String message, String errorCode, Map<String, String> fieldErrors) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    /**
     * Get field-level errors for Frontend display
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Check if has field-level errors
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
