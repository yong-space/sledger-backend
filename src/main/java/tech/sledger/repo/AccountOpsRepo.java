package tech.sledger.repo;

import tech.sledger.model.dto.Insight;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AccountOpsRepo {
    List<Map> getAccountsWithMetrics(long ownerId);
    List<String> getTopStrings(long ownerId, String field, String query);
    List<Insight> getInsights(long ownerId, Instant from, Instant to);
}
