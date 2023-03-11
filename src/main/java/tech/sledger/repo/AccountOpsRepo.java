package tech.sledger.repo;

import tech.sledger.model.account.AccountDTO;
import java.util.List;

public interface AccountOpsRepo {
    List<AccountDTO> getAccountsWithMetrics(long ownerId);
}
