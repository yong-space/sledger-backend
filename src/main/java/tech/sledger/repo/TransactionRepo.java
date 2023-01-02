package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Long> {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByAccountOrderByDate(Account account);
}
