package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InsightsResponse {
    private List<Insight> data;
    private List<CategoryInsight> summary;
}
