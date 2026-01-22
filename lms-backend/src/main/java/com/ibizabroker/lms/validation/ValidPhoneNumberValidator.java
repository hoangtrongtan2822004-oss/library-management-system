package com.ibizabroker.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // optional field; use @NotBlank to require
        String digits = value.trim();
        // Accepts numbers only starting with 0 and 10-11 digits total
        if (!digits.matches("^0\\d{9,10}$")) return false;
        return true;
    }
}
