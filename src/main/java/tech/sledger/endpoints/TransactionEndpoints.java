package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.TransactionService;
import tech.sledger.service.UserService;
import java.util.List;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionEndpoints {
    private final UserService userService;
    private final TransactionService txService;

    @PostMapping
    public Transaction addTransaction(Authentication auth, @RequestBody Transaction transaction) {
        userService.authorise(auth, transaction.getAccount().getId());
        return txService.add(List.of(transaction)).get(0);
    }

    @PostMapping("/{accountId}")
    public List<Transaction> addTransactions(
        Authentication auth,
        @PathVariable long accountId,
        @RequestBody List<Transaction> transactions
    ) {
        Account account = userService.authorise(auth, accountId);
        return txService.add(transactions.stream().peek(t -> t.setAccount(account)).toList());
    }

    @PutMapping
    public Transaction updateTransaction(Authentication auth, @RequestBody Transaction transaction) {
        userService.authorise(auth, transaction.getAccount().getId());
        return txService.edit(transaction);
    }

    @DeleteMapping("/{transactionId}")
    public void deleteTransaction(Authentication auth, @PathVariable long transactionId) {
        Transaction transaction = txService.get(transactionId);
        if (transaction == null) {
            throw new ResponseStatusException(NOT_FOUND, "No such transaction id");
        }
        userService.authorise(auth, transaction.getAccount().getId());
        txService.delete(transaction);
    }

    @GetMapping("/{accountId}")
    public List<Transaction> listTransactions(Authentication auth, @PathVariable long accountId) {
        Account account = userService.authorise(auth, accountId);
        return txService.list(account);
    }
}
