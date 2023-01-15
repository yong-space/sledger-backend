package tech.sledger.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoAliasedEmailValidator implements ConstraintValidator<NoAliasedEmails, String> {
    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        return s != null && !s.contains("+");
    }
}
