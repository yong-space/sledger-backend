package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.repo.TransactionRepo;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepo txRepo;

    public <T extends Transaction> T add(T transaction) {
        Transaction previous = txRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        transaction.setId(id);
        return txRepo.save(transaction);
    }

    public Transaction get(long id) {
        return txRepo.findById(id).orElse(null);
    }

    public List<Transaction> list(Account account) {
        return txRepo.findAllByAccountOrderByDate(account);
    }

    public <T extends Transaction> T save(T transaction) {
        return txRepo.save(transaction);
    }

    public void delete(Transaction transaction) {
        txRepo.delete(transaction);
    }
}
