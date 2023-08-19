package tech.sledger.repo;

import tech.sledger.model.dto.CreditCardStatement;

import java.util.List;

public interface TransactionOpsRepo {
    List<CreditCardStatement> getCreditCardStatement(long accountId);
}
