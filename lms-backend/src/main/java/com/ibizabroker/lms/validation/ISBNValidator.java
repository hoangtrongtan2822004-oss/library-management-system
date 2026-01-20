package com.ibizabroker.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 🔍 ISBN Checksum Validator Implementation
 * 
 * Validates ISBN-10 and ISBN-13 according to official checksum algorithms.
 */
public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {

    @Override
    public void initialize(ValidISBN constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        // Null và blank được coi là valid (dùng @NotNull/@NotBlank nếu muốn bắt buộc)
        if (isbn == null || isbn.isBlank()) {
            return true;
        }
        
        // Remove dấu gạch ngang và spaces
        String cleanIsbn = isbn.replaceAll("[-\\s]", "");
        
        // Check length
        if (cleanIsbn.length() == 10) {
            return isValidISBN10(cleanIsbn);
        } else if (cleanIsbn.length() == 13) {
            return isValidISBN13(cleanIsbn);
        } else {
            return false;
        }
    }
    
    /**
     * Validate ISBN-10 checksum
     * 
     * Algorithm:
     * 1. Multiply each digit by decreasing weight (10, 9, 8, ..., 2)
     * 2. Last digit can be 'X' (represents 10)
     * 3. Sum must be divisible by 11
     * 
     * Example: 0-306-40615-2
     * - (0×10 + 3×9 + 0×8 + 6×7 + 4×6 + 0×5 + 6×4 + 1×3 + 5×2 + 2×1) = 132
     * - 132 % 11 = 0 ✅
     */
    private boolean isValidISBN10(String isbn) {
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                if (!Character.isDigit(isbn.charAt(i))) {
                    return false;
                }
                int digit = Character.getNumericValue(isbn.charAt(i));
                sum += digit * (10 - i);
            }
            
            // Last character can be digit or 'X' (= 10)
            char lastChar = isbn.charAt(9);
            if (lastChar == 'X' || lastChar == 'x') {
                sum += 10;
            } else if (Character.isDigit(lastChar)) {
                sum += Character.getNumericValue(lastChar);
            } else {
                return false;
            }
            
            return (sum % 11 == 0);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate ISBN-13 checksum
     * 
     * Algorithm:
     * 1. Multiply odd position digits (1st, 3rd, 5th...) by 1
     * 2. Multiply even position digits (2nd, 4th, 6th...) by 3
     * 3. Sum must be divisible by 10
     * 
     * Example: 978-0-306-40615-7
     * - (9×1 + 7×3 + 8×1 + 0×3 + 3×1 + 0×3 + 6×1 + 4×3 + 0×1 + 6×3 + 1×1 + 5×3 + 7×1) = 100
     * - 100 % 10 = 0 ✅
     */
    private boolean isValidISBN13(String isbn) {
        try {
            int sum = 0;
            for (int i = 0; i < 13; i++) {
                if (!Character.isDigit(isbn.charAt(i))) {
                    return false;
                }
                int digit = Character.getNumericValue(isbn.charAt(i));
                // Odd position (0, 2, 4...) → weight 1
                // Even position (1, 3, 5...) → weight 3
                int weight = (i % 2 == 0) ? 1 : 3;
                sum += digit * weight;
            }
            
            return (sum % 10 == 0);
        } catch (Exception e) {
            return false;
        }
    }
}
