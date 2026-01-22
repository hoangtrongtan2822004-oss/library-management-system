package com.ibizabroker.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = DifferentPasswordsValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface DifferentPasswords {
    String message() default "Mật khẩu mới phải khác mật khẩu cũ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
