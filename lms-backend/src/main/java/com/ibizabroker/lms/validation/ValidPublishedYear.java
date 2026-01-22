package com.ibizabroker.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidPublishedYearValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
public @interface ValidPublishedYear {
    String message() default "Năm xuất bản không được lớn hơn năm hiện tại";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
