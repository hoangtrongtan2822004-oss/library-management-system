package com.ibizabroker.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class DifferentPasswordsValidator implements ConstraintValidator<DifferentPasswords, Object> {

    @Override
    public void initialize(DifferentPasswords constraintAnnotation) {
        // no-op
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        try {
            Method getOld = value.getClass().getMethod("getOldPassword");
            Method getNew = value.getClass().getMethod("getNewPassword");

            Object oldObj = getOld.invoke(value);
            Object newObj = getNew.invoke(value);

            if (oldObj == null || newObj == null) return true; // other validators handle null/blank

            return !oldObj.equals(newObj);
        } catch (NoSuchMethodException nsme) {
            // fallback: try fields via reflection - if not present, consider valid
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
