package com.ibizabroker.lms.validation;

import com.ibizabroker.lms.dao.UsersRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) return true; // @NotBlank/@Email handle required/format
        return usersRepository.findByEmailIgnoreCase(email).isEmpty();
    }
}
