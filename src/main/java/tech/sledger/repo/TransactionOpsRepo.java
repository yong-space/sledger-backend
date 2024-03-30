package tech.sledger.repo;

import tech.sledger.model.dto.CreditCardStatement;
import tech.sledger.model.dto.MonthlyBalance;

import java.time.Instant;
import java.util.List;

public interface TransactionOpsRepo {
    List<CreditCardStatement> getCreditCardBills(long accountId);
    List<MonthlyBalance> getBalanceHistory(List<Long> accountIds, Instant from, Instant to);
}
