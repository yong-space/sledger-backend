package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class MonthlyBalance {
    private Instant month;
    private long accountId;
    private BigDecimal balance;
}
