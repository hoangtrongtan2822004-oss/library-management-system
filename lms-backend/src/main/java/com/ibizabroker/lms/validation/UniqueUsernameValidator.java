package com.ibizabroker.lms.validation;

import com.ibizabroker.lms.dao.UsersRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) return true; // @NotBlank should handle required
        return !usersRepository.existsByUsernameIgnoreCase(username);
    }
}
