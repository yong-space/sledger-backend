package tech.sledger.service;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

    public <T extends Transaction> List<T> list(Account account) {
        return txRepo.findAllByAccountIdOrderByDate(account.getId());
    }

    public <T extends Transaction> List<T> listByDates(Account account, Instant from, Instant to) {
        return txRepo.findAllByAccountIdAndDateBetweenOrderByDate(account.getId(), from, to);
    }

    public <T extends Transaction> List<T> listAll(User user) {
        List<Long> accounts = accountRepo.findAllByOwnerAndTypeIn(user, List.of(Cash, Credit))
            .stream().map(Account::getId).toList();
        return txRepo.findAllByAccountIdInOrderByDate(accounts);
    }

    public List<? extends Transaction> listAll(User user, String remarks) {
        if (remarks.length() < 3) {
            throw new ResponseStatusException(BAD_REQUEST, "Remarks search must be at least 3 characters");
        }
        List<Long> accountIds = accountRepo.findAllByOwnerAndTypeIn(user, List.of(Cash, Credit))
            .stream().map(Account::getId).toList();
        return txRepo.findAllByAccountIdInAndRemarksContainingIgnoreCaseOrderByDate(accountIds, remarks);
    }

    public List<? extends Transaction> listAll(User user, String category, String subCategory, Instant from, Instant to) {
        List<Long> accountIds = accountRepo.findAllByOwnerAndTypeIn(user, List.of(Cash, Credit))
            .stream().map(Account::getId).toList();
        if (category == null && subCategory == null) {
            return txRepo.listAllByDateRange(accountIds, from, to);
        }
        if (category != null) {
            return txRepo.listAllByCategoryAndDateRange(accountIds, category, from, to);
        }
        return txRepo.listAllBySubCategoryAndDateRange(accountIds, subCategory, from, to);
    }

    @Transactional
    public <T extends Transaction> List<T> add(List<T> transactions) {
        Transaction previous = txRepo.findFirstByOrderByIdDesc();
        long idEpoch = (previous == null) ? 1 : previous.getId() + 1;
        return updateBalances(processDates(transactions, idEpoch), TxOperation.SAVE);
    }

    private <T extends Transaction> List<T> processDates(List<T> transactions, long idEpoch) {
        long accountId = transactions.getFirst().getAccountId();
        if (transactions.stream().anyMatch(t -> t.getAccountId() != accountId)) {
            throw new IllegalArgumentException("All transactions must belong to the same account");
        }

        Instant rangeAfter = transactions.stream().min(comparing(Transaction::getDate))
            .map(t -> t.getDate().minus(1, ChronoUnit.MILLIS)).orElseThrow();
        Instant rangeBefore = transactions.stream().max(comparing(Transaction::getDate))
            .map(t -> t.getDate().plus(1, ChronoUnit.DAYS)).orElseThrow();

        // Date-only projection avoids loading full documents just to sequence timestamps
        Map<LocalDate, Instant> dayMaxDate = txRepo.findDatesByAccountIdAndDateBetween(accountId, rangeAfter, rangeBefore)
            .stream()
            .collect(Collectors.toMap(
                t -> t.getDate().atZone(ZoneOffset.UTC).toLocalDate(),
                Transaction::getDate,
                (a, b) -> a.isAfter(b) ? a : b
            ));

        log.debug("Day max dates in range {}-{}: {}", rangeAfter, rangeBefore, dayMaxDate);

        long id = idEpoch;
        List<T> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparing(Transaction::getDate));

        for (T transaction : sorted) {
            transaction.setId(id++);
            LocalDate targetDay = transaction.getDate().atZone(ZoneOffset.UTC).toLocalDate();
            Instant maxExisting = dayMaxDate.get(targetDay);

            Instant finalDate;
            if (maxExisting != null) {
                finalDate = maxExisting.plus(1L, ChronoUnit.SECONDS);
                log.debug("Updated date to: {}", finalDate);
            } else {
                finalDate = targetDay.atStartOfDay(ZoneOffset.UTC).toInstant();
                log.debug("No existing date found. Maintain {}", finalDate);
            }
            transaction.setDate(finalDate);
            // Track this tx so subsequent same-batch transactions chain off it
            dayMaxDate.put(targetDay, finalDate);
        }
        return sorted;
    }

    @Transactional
    public <T extends Transaction> List<T> edit(List<T> transactions) {
        Map<Long, Transaction> existingById = txRepo.findAllById(
                transactions.stream().map(Transaction::getId).toList())
            .stream().collect(Collectors.toMap(Transaction::getId, t -> t));

        boolean hasBalanceImpact = transactions.stream()
            .anyMatch(t -> {
                Transaction existing = existingById.get(t.getId());
                return t.getAmount().compareTo(existing.getAmount()) != 0
                    || !t.getDate().equals(existing.getDate());
            });

        if (!hasBalanceImpact) {
            // No balance impact — reuse the already-loaded balances, no second DB fetch
            long accountId = transactions.getFirst().getAccountId();
            transactions.forEach(t -> t.setBalance(existingById.get(t.getId()).getBalance()));
            cache.clearTxCache(accountId);
            return txRepo.saveAll(transactions);
        }

        return updateBalancesForEdit(transactions, existingById);
    }

    @Transactional
    public <T extends Transaction> List<T> editAsIs(List<T> transactions) {
        cache.clearTxCache(transactions.getFirst().getAccountId());
        Map<Long, BigDecimal> balances = txRepo.findAllById(transactions.stream().map(Transaction::getId).toList())
            .stream().collect(Collectors.toMap(Transaction::getId, Transaction::getBalance));
        transactions.forEach(t -> t.setBalance(balances.get(t.getId())));
        return txRepo.saveAll(transactions);
    }

    @Transactional
    public void delete(List<Transaction> transactions) {
        txRepo.deleteAll(transactions);
        updateBalances(transactions, TxOperation.REMOVE);
    }

    // Edit-specific recalculation: starts from min(new dates, old dates) so that
    // transactions between an old and new position are always recomputed correctly.
    // Uses all edited IDs for epoch exclusion rather than just the minDate transaction.
    @SuppressWarnings("unchecked")
    private <T extends Transaction> List<T> updateBalancesForEdit(
            List<T> transactions, Map<Long, Transaction> existingById) {
        long accountId = transactions.getFirst().getAccountId();
        List<Long> txIds = transactions.stream().map(Transaction::getId).toList();

        Instant minNewDate = transactions.stream().min(comparing(Transaction::getDate))
            .map(Transaction::getDate).orElseThrow();
        Instant minOldDate = txIds.stream()
            .map(id -> existingById.get(id).getDate())
            .min(Comparator.naturalOrder()).orElseThrow();
        Instant minDate = minNewDate.isBefore(minOldDate) ? minNewDate : minOldDate;

        Transaction epoch = txRepo.findFirstByAccountIdAndIdNotInAndDateBeforeOrderByDateDesc(accountId, txIds, minDate);

        List<T> affectedTx = new ArrayList<>(transactions);
        affectedTx.addAll((List<T>) txRepo.findAllByAccountIdAndDateAfterExcludingIds(accountId, minDate, txIds));
        affectedTx.sort(Comparator.comparing(Transaction::getDate));

        BigDecimal balance = epoch != null ? epoch.getBalance() : BigDecimal.ZERO;
        for (T t : affectedTx) {
            balance = balance.add(t.getAmount());
            t.setBalance(balance);
        }
        txRepo.saveAll(affectedTx);
        cache.clearTxCache(accountId);
        return transactions;
    }

    @SuppressWarnings("unchecked")
    private <T extends Transaction> List<T> updateBalances(List<T> transactions, TxOperation op) {
        T minTx = transactions.stream().min(comparing(Transaction::getDate)).orElseThrow();
        Instant minDate = minTx.getDate();
        long accountId = transactions.getFirst().getAccountId();
        List<Long> txIds = transactions.stream().map(Transaction::getId).toList();

        List<T> affectedTx = new ArrayList<>();
        if (op == TxOperation.SAVE) {
            affectedTx.addAll(transactions);
        }
        // DB-side ID exclusion avoids loading and filtering in Java
        var txAfterEpoch = (List<T>) txRepo.findAllByAccountIdAndDateAfterExcludingIds(accountId, minDate, txIds);
        affectedTx.addAll(txAfterEpoch);
        affectedTx.sort(Comparator.comparing(Transaction::getDate));

        Transaction epoch = txRepo.findFirstByAccountIdAndIdNotAndDateBeforeOrderByDateDesc(accountId, minTx.getId(), minDate);
        BigDecimal balance = epoch != null ? epoch.getBalance() : BigDecimal.ZERO;
        for (T t : affectedTx) {
            balance = balance.add(t.getAmount());
            t.setBalance(balance);
        }
        txRepo.saveAll(affectedTx);
        cache.clearTxCache(accountId);
        return transactions;
    }
}
