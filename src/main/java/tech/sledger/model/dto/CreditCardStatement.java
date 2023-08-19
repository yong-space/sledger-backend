package tech.sledger.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditCardStatement(
    Instant month,
    BigDecimal amount,
    int transactions
) {}
