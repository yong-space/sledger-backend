package tech.sledger.model.portfolio;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
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
