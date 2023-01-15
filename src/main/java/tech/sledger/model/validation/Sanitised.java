package tech.sledger.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SanitiseValidator.class)
@Documented
public @interface Sanitised {
    String message() default "Fields should only contain text content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
