package tech.sledger.repo;

import java.util.List;
import java.util.Map;

public interface AccountOpsRepo {
    List<Map> getAccountsWithMetrics(long ownerId);
    List<String> getTopStrings(long ownerId, String field, String query);
}
