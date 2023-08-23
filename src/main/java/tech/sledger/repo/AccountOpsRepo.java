package tech.sledger.repo;

import tech.sledger.model.dto.AccountDTO;
import tech.sledger.model.dto.CategorySuggestion;
import tech.sledger.model.dto.Insight;
import java.time.Instant;
import java.util.List;

public interface AccountOpsRepo {
    List<AccountDTO> getAccountsWithMetrics(long ownerId);
    List<String> getTopStrings(long ownerId, String field, String query);
    List<Insight> getInsights(long ownerId, Instant from, Instant to);
    List<CategorySuggestion> getCategories(long ownerId);
}
