package tech.sledger.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoAliasedEmailValidator.class)
@Documented
public @interface NoAliasedEmails {
    String message() default "No aliased emails allowed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
