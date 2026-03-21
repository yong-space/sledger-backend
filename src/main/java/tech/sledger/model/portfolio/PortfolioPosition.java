package tech.sledger.model.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPosition {
    private String ticker;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal dailyPnl;
    private BigDecimal changePercent;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPercent;
    private BigDecimal mktValue;
}
