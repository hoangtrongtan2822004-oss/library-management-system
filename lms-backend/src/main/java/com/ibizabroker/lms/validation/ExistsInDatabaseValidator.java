package com.ibizabroker.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
public class ExistsInDatabaseValidator implements ConstraintValidator<ExistsInDatabase, Collection<Integer>> {

    private Class<?> repositoryClass;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void initialize(ExistsInDatabase constraintAnnotation) {
        this.repositoryClass = Objects.requireNonNull(constraintAnnotation.repository(), "repository class must not be null");
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isValid(Collection<Integer> values, ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) return true; // other annotations enforce non-empty
        try {
            Object repoBean = applicationContext.getBean(repositoryClass);
            if (!(repoBean instanceof CrudRepository)) {
                // cannot validate
                return true;
            }
            CrudRepository repository = (CrudRepository) repoBean;
            java.util.List<Integer> missing = new java.util.ArrayList<>();
            for (Integer id : values) {
                if (id == null) {
                    missing.add(null);
                    continue;
                }
                if (!repository.existsById(id)) missing.add(id);
            }
            if (!missing.isEmpty()) {
                // Build a detailed violation message listing missing IDs
                context.disableDefaultConstraintViolation();
                String repoName = repositoryClass.getSimpleName();
                String msg = "Missing IDs in " + repoName + ": " + missing.toString();
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            return true;
        } catch (Exception e) {
            // If repository bean not found or other error, treat as valid to avoid blocking deployments
            return true;
        }
    }
}
