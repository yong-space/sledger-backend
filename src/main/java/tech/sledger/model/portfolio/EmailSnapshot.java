package tech.sledger.model.portfolio;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EmailSnapshot {
    private List<PortfolioPosition> positions;
    private int positionCount;
    private BigDecimal dayPnl;
    private BigDecimal dayPnlPercent;
    private BigDecimal netPnl;
    private BigDecimal netPnlPercent;
    private BigDecimal mktValue;
}
