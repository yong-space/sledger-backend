package tech.sledger.repo;

import java.util.Map;

public interface TransactionOpsRepo {
    Map<Long, Long> countTransactionsByAccount(long ownerId);
}
