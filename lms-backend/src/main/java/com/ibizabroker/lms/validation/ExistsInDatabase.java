package com.ibizabroker.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ExistsInDatabaseValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
public @interface ExistsInDatabase {
    String message() default "Một hoặc nhiều ID không tồn tại trong cơ sở dữ liệu";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Repository class to use for existence check. Example: AuthorRepository.class
     */
    Class<?> repository();
}
