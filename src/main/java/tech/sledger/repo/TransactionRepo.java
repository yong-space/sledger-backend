package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.Transaction;
import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long>, TransactionOpsRepo {
    <T extends Transaction> T findFirstByOrderByIdDesc();
    <T extends Transaction> List<T> findAllByAccountId(long accountId);
    <T extends Transaction> List<T> findAllByAccountIdOrderByDate(long accountId);
    @Query(value = "{ 'accountId': ?0, 'date': { $gte: ?1, $lte: ?2 } }", sort = "{ 'date': 1 }")
    <T extends Transaction> List<T> findAllByAccountIdAndDateBetweenOrderByDate(long accountId, Instant from, Instant to);
    <T extends Transaction> List<T> findAllByAccountIdInOrderByDate(List<Long> accountIds);
    <T extends Transaction> T findFirstByAccountIdAndIdNotAndDateBeforeOrderByDateDesc(long accountId, long id, Instant date);
    <T extends Transaction> List<T> findAllByAccountIdAndDateAfterOrderByDate(long accountId, Instant date);
    List<? extends CashTransaction> findAllByAccountIdInAndRemarksContainingIgnoreCaseOrderByDate(List<Long> accountIds, String remarks);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'date': { $gte: ?1, $lte: ?2 } }", sort = "{ 'date': 1 }")
    List<? extends CashTransaction> listAllByDateRange(List<Long> accountIds, Instant from, Instant to);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'category': ?1, 'date': { $gte: ?2, $lte: ?3 } }", sort = "{ 'date': 1 }")
    List<? extends CashTransaction> listAllByCategoryAndDateRange(List<Long> accountIds, String category, Instant from, Instant to);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'subCategory': ?1, 'date': { $gte: ?2, $lte: ?3 } }", sort = "{ 'date': 1 }")
    List<? extends CashTransaction> listAllBySubCategoryAndDateRange(List<Long> accountIds, String subCategory, Instant from, Instant to);
}
