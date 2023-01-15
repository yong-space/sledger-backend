package tech.sledger.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ComplexPasswordValidator implements ConstraintValidator<ComplexPassword, String> {
    private static final String uppercasePattern = ".*[A-Z].*";
    private static final String lowercasePattern = ".*[a-z].*";
    private static final String numberPattern = ".*\\d.*";
    private static final String symbolPattern = ".*\\W.*";

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        return s != null
            && s.matches(uppercasePattern)
            && s.matches(lowercasePattern)
            && s.matches(numberPattern)
            && s.matches(symbolPattern);
    }
}
