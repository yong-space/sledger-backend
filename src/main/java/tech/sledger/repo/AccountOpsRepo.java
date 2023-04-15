package tech.sledger.repo;

import java.util.List;
import java.util.Map;

public interface AccountOpsRepo {
    List<Map> getAccountsWithMetrics(long ownerId);
    List<String> getTopRemarks(long ownerId, String q);
    List<String> getTopCategories(long ownerId, String q);
}
