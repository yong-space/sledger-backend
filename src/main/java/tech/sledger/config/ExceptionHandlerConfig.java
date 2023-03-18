package tech.sledger.config;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.Objects;

@RestControllerAdvice
public class ExceptionHandlerConfig extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handle(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatusCode(), Objects.requireNonNull(e.getReason()));
    }
}
