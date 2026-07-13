package tech.sledger.endpoints;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.dto.BulkTransactionUpdate;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Transaction;
import tech.sledger.model.user.User;
import tech.sledger.service.TransactionService;
import tech.sledger.service.UserService;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionEndpoints {
    private final UserService userService;
    private final TransactionService txService;

    @PostMapping
    public List<Transaction> addTransactions(
        Authentication auth,
        @RequestBody List<Transaction> transactions
    ) {
        bulkAuthorise(auth, transactions);
        return txService.add(transactions);
    }

    @PutMapping
    public List<Transaction> updateTransactions(
        Authentication auth,
        @RequestBody List<Transaction> transactions
    ) {
        bulkValidateAndAuthorise(auth, transactions.stream().map(Transaction::getId).toList());
        return txService.edit(transactions);
    }

    @PutMapping("/bulk")
    public List<Transaction> bulkUpdateTransactions(
        Authentication auth,
        @RequestBody BulkTransactionUpdate update
    ) {
        bulkValidateAndAuthoriseMultiAccounts(auth, update.ids());
        List<Transaction> transactions = txService.get(update.ids());
        for (Transaction t : transactions) {
            if (t instanceof CashTransaction cashTx) {
                if (update.category() != null) {
                    cashTx.setCategory(update.category());
                }
                if (update.subCategory() != null) {
                    cashTx.setSubCategory(update.subCategory());
                }
                if (update.remarks() != null) {
                    cashTx.setRemarks(update.remarks());
                }
            }
            if (t instanceof CreditTransaction creditTx) {
                if (update.billingMonth() != null) {
                    creditTx.setBillingMonth(update.billingMonth());
                }
            }
        }
        return txService.editAsIs(transactions);
    }

    @DeleteMapping("/{transactionIds}")
    public void deleteTransactions(Authentication auth, @PathVariable("transactionIds") List<Long> transactionIds) {
        txService.delete(bulkValidateAndAuthorise(auth, transactionIds));
    }

    @GetMapping("/{accountId}")
    public List<? extends Transaction> listTransactions(
        Authentication auth,
        @PathVariable("accountId") long accountId,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String subCategory,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
        if (accountId == 0) {
            User user = (User) auth.getPrincipal();
            if (q != null) {
                return txService.listAll(user, q);
            }
            if (from != null && to != null) {
                if (subCategory != null) {
                    return txService.listAll(user, null, subCategory, from, to);
                }
                if (category != null) {
                    return txService.listAll(user, category, null, from, to);
                }
                return txService.listAll(user, null, null, from, to);
            }
            if (category != null || subCategory != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Date range is required");
            }
            return txService.listAll(user);
        }
        Account account = userService.authorise(auth, accountId);
        if (from != null && to != null) {
            return txService.listByDates(account, from, to);
        }
        return txService.list(account);
    }

    private List<Transaction> bulkValidateAndAuthorise(Authentication auth, List<Long> transactionIds) {
        List<Transaction> transactions = txService.get(transactionIds);
        if (transactionIds.size() != transactions.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Some transaction ids are not valid");
        }
        return bulkAuthorise(auth, transactions);
    }

    private void bulkValidateAndAuthoriseMultiAccounts(Authentication auth, List<Long> transactionIds) {
        List<Transaction> transactions = txService.get(transactionIds);
        if (transactionIds.size() != transactions.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Some transaction ids are not valid");
        }
        transactions.stream()
            .map(Transaction::getAccountId)
            .distinct().forEach(accountId -> userService.authorise(auth, accountId));
    }

    private <T extends Transaction> List<T> bulkAuthorise(Authentication auth, List<T> transactions) {
        List<Long> accounts = transactions.stream().map(Transaction::getAccountId).distinct().toList();
        if (accounts.size() > 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Bulk operations can only be performed on transactions under the same account");
        }
        userService.authorise(auth, accounts.getFirst());
        return transactions;
    }
}
