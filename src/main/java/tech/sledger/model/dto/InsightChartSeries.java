package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InsightChartSeries {
    private String id;
    private String label;
    private List<BigDecimal> data;
    private String stack;
    @Builder.Default
    private String stackOrder = "descending";
}