package tech.sledger.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ComplexPasswordValidator.class)
@Documented
public @interface ComplexPassword {
    String message() default "Password should contain at least 1 uppercase, 1 lowercase, 1 numeric and 1 special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
