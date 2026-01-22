package com.ibizabroker.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
public @interface PasswordStrength {
    String message() default "Mật khẩu phải chứa chữ hoa, chữ thường, chữ số và ký tự đặc biệt";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
