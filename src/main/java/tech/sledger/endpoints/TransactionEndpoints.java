package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.TransactionService;
import tech.sledger.service.UserService;
import java.util.List;

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
        return txService.save(transaction);
    }

    @PutMapping
    public Transaction updateTransaction(Authentication auth, @RequestBody Transaction transaction) {
        userService.authorise(auth, transaction.getAccount().getId());
        return txService.save(transaction);
    }

    @DeleteMapping("/{transactionId}")
    public void deleteAccount(Authentication auth, @PathVariable long transactionId) {
        Transaction transaction = txService.get(transactionId);
        userService.authorise(auth, transaction.getAccount().getId());
        txService.delete(transaction);
    }

    @GetMapping("/{accountId}")
    public List<Transaction> listTransactions(Authentication auth, @PathVariable long accountId) {
        Account account = userService.authorise(auth, accountId);
        return txService.list(account);
    }
}
