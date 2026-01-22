package com.ibizabroker.lms.validation;

import com.ibizabroker.lms.dao.RoleRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ValidRolesValidator implements ConstraintValidator<ValidRoles, Collection<String>> {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public boolean isValid(Collection<String> roles, ConstraintValidatorContext context) {
        if (roles == null || roles.isEmpty()) return true; // optional, default role applied elsewhere
        for (String r : roles) {
            if (r == null || r.isBlank()) return false;
            if (roleRepository.findByRoleName(r).isEmpty()) return false;
        }
        return true;
    }
}
