package tech.sledger.model.dto;

import org.springframework.data.annotation.Id;
import java.math.BigDecimal;
import java.time.Instant;

public record CreditCardStatement(
    @Id
    Instant month,
    BigDecimal amount,
    int transactions
) {}
