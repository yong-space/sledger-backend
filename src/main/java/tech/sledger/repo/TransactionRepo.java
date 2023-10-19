package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.tx.Transaction;

import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long>, TransactionOpsRepo {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByAccountId(long accountId);
    List<Transaction> findAllByAccountIdOrderByDate(long accountId);
    List<Transaction> findAllByAccountIdInOrderByDate(List<Long> accountIds);
    Transaction findFirstByAccountIdAndIdNotAndDateBeforeOrderByDateDesc(long accountId, long id, Instant date);
    List<Transaction> findAllByAccountIdAndDateAfterOrderByDate(long accountId, Instant date);
    List<Transaction> findAllByAccountIdAndDateBetween(long accountId, Instant after, Instant before);
}
