package tech.sledger.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insight {
    private Instant month;
    private String category;
    private String subCategory;
    private int transactions;
    private BigDecimal total;
}
