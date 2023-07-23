package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.tx.Transaction;

import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long> {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByAccountId(long accountId);
    List<Transaction> findAllByAccountIdOrderByDate(long accountId);
    Transaction findFirstByAccountIdAndDateBeforeOrderByDateDesc(long accountId, Instant date);
    Transaction findFirstByAccountIdAndDateBetweenOrderByDateDesc(long accountId, Instant before, Instant after);
    List<Transaction> findAllByAccountIdAndDateAfter(long accountId, Instant date);
    List<Transaction> findAllByAccountIdAndDateBetween(long accountId, Instant after, Instant before);
}
