package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChartResponse {
    private List<Instant> xAxis;
    private List<ChartSeries> series;
    private List<?> summary;
}
