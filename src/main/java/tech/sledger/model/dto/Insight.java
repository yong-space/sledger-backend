package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Insight {
    private Instant month;
    private String category;
    private String subCategory;
    private int transactions;
    private BigDecimal total;
}
