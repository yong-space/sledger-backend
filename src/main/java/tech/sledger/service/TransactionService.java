package tech.sledger.service;

import static java.util.Comparator.comparing;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountRepo accountRepo;
    private final TransactionRepo txRepo;
    private final CacheService cache;

    enum TxOperation { SAVE, REMOVE }

    public List<Transaction> get(List<Long> ids) {
        return txRepo.findAllById(ids);
    }

    @Cacheable(value="tx", key="#account.id")
    public List<Transaction> list(Account account) {
        return txRepo.findAllByAccountIdOrderByDate(account.getId());
    }

    public List<Transaction> listAll(User user) {
        List<Long> accounts = accountRepo.findAllByOwnerAndTypeIn(user, List.of(Cash, Credit))
            .stream().map(Account::getId).toList();
        return txRepo.findAllByAccountIdInOrderByDate(accounts);
    }

    @Transactional
    public <T extends Transaction> List<T> add(List<T> transactions) {
        Transaction previous = txRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;

        // Get minimum date in new transactions - 1 ms
        Instant rangeAfter = transactions.stream().min(comparing(Transaction::getDate))
            .map(t -> t.getDate().minus(1, ChronoUnit.MILLIS)).orElseThrow();
        // Get maximum date in new transactions + 1 day
        Instant rangeBefore = transactions.stream().max(comparing(Transaction::getDate))
            .map(t -> t.getDate().plus(1, ChronoUnit.DAYS)).orElseThrow();
        long accountId = transactions.get(0).getAccountId();
        // Get existing transactions in range
        List<Transaction> existing = txRepo.findAllByAccountIdAndDateBetween(accountId, rangeAfter, rangeBefore);

        transactions = new ArrayList<>(transactions);
        transactions.sort(Comparator.comparing(Transaction::getDate));
        for (Transaction transaction : transactions) {
            transaction.setId(id++);
            Instant targetDate = transaction.getDate().atZone(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS).toInstant();
            Instant before = targetDate.minus(1, ChronoUnit.MILLIS);
            Instant after = before.plus(1, ChronoUnit.DAYS);

            // Filter existing transactions to find same day transaction
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
    public <T extends Transaction> List<T> edit(List<T> transactions) {
        return updateBalances(transactions, TxOperation.SAVE);
    }

    @Transactional
    public <T extends Transaction> List<T> editAsIs(List<T> transactions) {
        cache.clearTxCache(transactions.get(0).getAccountId());
        return txRepo.saveAll(transactions);
    }

    @Transactional
    public void delete(List<Transaction> transactions) {
        txRepo.deleteAll(transactions);
        updateBalances(transactions, TxOperation.REMOVE);
    }

    @SuppressWarnings("unchecked")
    private <T extends Transaction> List<T> updateBalances(List<T> transactions, TxOperation op) {
        Instant minDate = transactions.stream().min(comparing(Transaction::getDate))
            .map(Transaction::getDate).orElseThrow();
        long accountId = transactions.get(0).getAccountId();
        Transaction epoch = txRepo.findFirstByAccountIdAndDateBeforeOrderByDateDesc(accountId, minDate);

        BigDecimal balance = epoch != null ? epoch.getBalance() : BigDecimal.ZERO;

        List<T> affectedTx = new ArrayList<>();
        if (op == TxOperation.SAVE) {
            affectedTx.addAll(transactions);
        }
        affectedTx.addAll((Collection<? extends T>) txRepo.findAllByAccountIdAndDateAfter(accountId, minDate));

        for (T t : affectedTx) {
            balance = balance.add(t.getAmount());
            t.setBalance(balance);
        }
        txRepo.saveAll(affectedTx);
        cache.clearTxCache(accountId);
        return transactions;
    }
}
