package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ChartSeries {
    private String id;
    private String label;
    private List<BigDecimal> data;
    private String stack;
    @Builder.Default
    private String stackOrder = "descending";
    @Builder.Default
    private String type = "bar";
    @Builder.Default
    private HighlightScope highlightScope = new HighlightScope("series", "global");

    record HighlightScope(String highlighted, String faded) {}
}
