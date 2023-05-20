package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long> {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByAccount(Account account);
    List<Transaction> findAllByAccountOrderByDate(Account account);
    Transaction findFirstByAccountAndDateBeforeOrderByDateDesc(Account account, Instant date);
    Transaction findFirstByAccountAndDateBetweenOrderByDateDesc(Account account, Instant before, Instant after);
    List<Transaction> findAllByAccountAndDateAfter(Account account, Instant date);
    List<Transaction> findAllByAccountAndDateBetween(Account account, Instant after, Instant before);
}
