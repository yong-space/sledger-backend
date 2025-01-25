package tech.sledger.model.portfolio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioSummary {
    private String broker;
    private String brokerColour;
    private BigDecimal holdings;
    private BigDecimal cash;
    private BigDecimal fx;
}
