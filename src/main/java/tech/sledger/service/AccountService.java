package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;
    private final TransactionRepo txRepo;

    public Account add(Account account) {
        Account previous = accountRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        account.setId(id);

        if (account instanceof CreditAccount creditAccount && creditAccount.getPaymentAccountId() > 0) {
            Account paymentAccount = get(creditAccount.getPaymentAccountId());
            if (paymentAccount == null || !account.getOwner().equals(paymentAccount.getOwner())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment account id");
            }
        }
        return accountRepo.save(account);
    }

    public Account edit(Account account) {
        return accountRepo.save(account);
    }

    public Account get(long id) {
        return accountRepo.findById(id).orElse(null);
    }

    public List<Map> list(User owner) {
        return accountRepo.getAccountsWithMetrics(owner.getId());
    }

    public void delete(Account account) {
        txRepo.deleteAll(txRepo.findAllByAccount(account));
        accountRepo.delete(account);
    }
}
