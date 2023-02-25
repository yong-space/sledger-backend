package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long>, TransactionOpsRepo {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByAccount(Account account);
    List<Transaction> findAllByAccountOrderByDate(Account account);
    Transaction findFirstByDateBeforeOrderByDateDesc(Instant date);
    Transaction findFirstByDateBetweenOrderByDateDesc(Instant before, Instant after);
    List<? extends Transaction> findAllByDateAfter(Instant date);
}
