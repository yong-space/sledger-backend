package tech.sledger.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SanitiseValidator implements ConstraintValidator<Sanitised, String> {
    private static final String invalidPattern = ".*[<;>].*";

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        return s != null && !s.matches(invalidPattern);
    }
}
