package com.ibizabroker.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

public class ValidPublishedYearValidator implements ConstraintValidator<ValidPublishedYear, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true; // optional
        int current = Year.now().getValue();
        return value <= current;
    }
}
