package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.repo.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepo txRepo;

    enum TxOperation { SAVE, REMOVE }

    public Transaction get(long id) {
        return txRepo.findById(id).orElse(null);
    }

    public List<Transaction> list(Account account) {
        return txRepo.findAllByAccountOrderByDate(account);
    }

    @Transactional
    public <T extends Transaction> T add(T transaction) {
        Transaction previous = txRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        transaction.setId(id);

        Instant targetDate = transaction.getDate().atZone(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant before = targetDate.minus(1, ChronoUnit.MILLIS);
        Instant after = before.plus(1, ChronoUnit.DAYS);
        Transaction sameDate = txRepo.findFirstByDateBetweenOrderByDateDesc(before, after);
        if (sameDate != null) {
            targetDate = sameDate.getDate().plus(1L, ChronoUnit.SECONDS);
        }
        transaction.setDate(targetDate);
        return updateBalances(transaction, TxOperation.SAVE);
    }

    @Transactional
    public <T extends Transaction> T edit(T transaction) {
        return updateBalances(transaction, TxOperation.SAVE);
    }

    @Transactional
    public void delete(Transaction transaction) {
        txRepo.delete(transaction);
        updateBalances(transaction, TxOperation.REMOVE);
    }

    @SuppressWarnings("unchecked")
    private <T extends Transaction> T updateBalances(T transaction, TxOperation op) {
        Instant date = transaction.getDate();
        Transaction epoch = txRepo.findFirstByDateBeforeOrderByDateDesc(date);

        BigDecimal balance = epoch != null ? epoch.getBalance() : BigDecimal.ZERO;

        List<T> affectedTx = new ArrayList<>();
        if (op == TxOperation.SAVE) {
            affectedTx.add(transaction);
        }
        affectedTx.addAll((Collection<? extends T>) txRepo.findAllByDateAfter(date));

        for (T t : affectedTx) {
            balance = balance.add(t.getAmount());
            t.setBalance(balance);
        }
        txRepo.saveAll(affectedTx);

        return transaction;
    }
}
