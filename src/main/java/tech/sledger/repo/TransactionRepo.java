package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.tx.Transaction;

import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long>, TransactionOpsRepo {
    <T extends Transaction> T findFirstByOrderByIdDesc();
    <T extends Transaction> List<T> findAllByAccountId(long accountId);
    <T extends Transaction> List<T> findAllByAccountIdOrderByDate(long accountId);
    <T extends Transaction> List<T> findAllByAccountIdInOrderByDate(List<Long> accountIds);
    <T extends Transaction> T findFirstByAccountIdAndIdNotAndDateBeforeOrderByDateDesc(long accountId, long id, Instant date);
    <T extends Transaction> List<T> findAllByAccountIdAndDateAfterOrderByDate(long accountId, Instant date);
    <T extends Transaction> List<T> findAllByAccountIdAndDateBetween(long accountId, Instant after, Instant before);
}
