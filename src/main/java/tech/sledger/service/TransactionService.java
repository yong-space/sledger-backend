package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.repo.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static java.util.Comparator.comparing;

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
    public <T extends Transaction> List<T> add(List<T> transactions) {
        Transaction previous = txRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;

        Instant rangeAfter = transactions.stream().min(comparing(Transaction::getDate))
            .map(t -> t.getDate().minus(1, ChronoUnit.MILLIS)).orElseThrow();
        Instant rangeBefore = transactions.stream().max(comparing(Transaction::getDate))
            .map(t -> t.getDate().plus(1, ChronoUnit.DAYS)).orElseThrow();
        Account account = transactions.get(0).getAccount();
        List<Transaction> existing = txRepo.findAllByAccountAndDateBetween(account, rangeAfter, rangeBefore);

        transactions = new ArrayList<>(transactions);
        transactions.sort(Comparator.comparing(Transaction::getDate));
        for (Transaction transaction : transactions) {
            transaction.setId(id++);
            Instant targetDate = transaction.getDate().atZone(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS).toInstant();
            Instant before = targetDate.minus(1, ChronoUnit.MILLIS);
            Instant after = before.plus(1, ChronoUnit.DAYS);

            Instant existingDate = existing.stream()
                .filter(t -> t.getDate().isAfter(before) && t.getDate().isBefore(after))
                .max(Comparator.comparing(Transaction::getDate))
                .map(Transaction::getDate).orElse(null);
            if (existingDate != null) {
                targetDate = existingDate.plus(1L, ChronoUnit.SECONDS);
            }
            transaction.setDate(targetDate);
            existing.add(transaction);
        }
        return updateBalances(transactions, TxOperation.SAVE);
    }

    @Transactional
    public <T extends Transaction> T edit(T transaction) {
        return updateBalances(List.of(transaction), TxOperation.SAVE).get(0);
    }

    @Transactional
    public void delete(Transaction transaction) {
        txRepo.delete(transaction);
        updateBalances(List.of(transaction), TxOperation.REMOVE);
    }

    @SuppressWarnings("unchecked")
    private <T extends Transaction> List<T> updateBalances(List<T> transactions, TxOperation op) {
        Instant minDate = transactions.stream().min(comparing(Transaction::getDate))
            .map(Transaction::getDate).orElseThrow();
        Account account = transactions.get(0).getAccount();
        Transaction epoch = txRepo.findFirstByAccountAndDateBeforeOrderByDateDesc(account, minDate);

        BigDecimal balance = epoch != null ? epoch.getBalance() : BigDecimal.ZERO;

        List<T> affectedTx = new ArrayList<>();
        if (op == TxOperation.SAVE) {
            affectedTx.addAll(transactions);
        }
        affectedTx.addAll((Collection<? extends T>) txRepo.findAllByAccountAndDateAfter(account, minDate));

        for (T t : affectedTx) {
            balance = balance.add(t.getAmount());
            t.setBalance(balance);
        }
        txRepo.saveAll(affectedTx);

        return transactions;
    }
}
