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
    @Query(value = "{ 'accountId': ?0, 'date': { $gte: ?1, $lte: ?2 } }", fields = "{ 'date': 1 }")
    List<Transaction> findDatesByAccountIdAndDateBetween(long accountId, Instant from, Instant to);
    <T extends Transaction> List<T> findAllByAccountIdInOrderByDate(List<Long> accountIds);
    <T extends Transaction> T findFirstByAccountIdAndIdNotAndDateBeforeOrderByDateDesc(long accountId, long id, Instant date);
    <T extends Transaction> T findFirstByAccountIdAndIdNotInAndDateBeforeOrderByDateDesc(long accountId, List<Long> ids, Instant date);
    @Query("{ 'accountId': ?0, 'date': { $gt: ?1 }, '_id': { $nin: ?2 } }")
    <T extends Transaction> List<T> findAllByAccountIdAndDateAfterExcludingIds(long accountId, Instant date, List<Long> ids);
    @Query("{ 'accountId': { $in: ?0 }, 'remarks': { $regex: ?1, $options: 'i' } }")
    <T extends CashTransaction> List<T> findAllByAccountIdInAndRemarksContainingIgnoreCaseOrderByDate(List<Long> accountIds, String remarks);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'date': { $gte: ?1, $lte: ?2 } }", sort = "{ 'date': 1 }")
    <T extends CashTransaction> List<T> listAllByDateRange(List<Long> accountIds, Instant from, Instant to);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'category': ?1, 'date': { $gte: ?2, $lte: ?3 } }", sort = "{ 'date': 1 }")
    <T extends CashTransaction> List<T> listAllByCategoryAndDateRange(List<Long> accountIds, String category, Instant from, Instant to);
    @Query(value = "{ 'accountId': { $in: ?0 }, 'subCategory': ?1, 'date': { $gte: ?2, $lte: ?3 } }", sort = "{ 'date': 1 }")
    <T extends CashTransaction> List<T> listAllBySubCategoryAndDateRange(List<Long> accountIds, String subCategory, Instant from, Instant to);
}
