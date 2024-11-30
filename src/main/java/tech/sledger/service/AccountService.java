package tech.sledger.service;

import static tech.sledger.endpoints.AccountEndpoints.SortDirection.down;
import static tech.sledger.endpoints.AccountEndpoints.SortDirection.up;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.endpoints.AccountEndpoints;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.dto.AccountDTO;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;
    private final TransactionRepo txRepo;
    private final CacheService cache;

    public Account add(Account account) {
        Account previous = accountRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        account.setId(id);
        account.setSortOrder((previous == null) ? 0 : previous.getSortOrder() + 1);

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

    public List<AccountDTO> list(User owner) {
        return accountRepo.getAccountsWithMetrics(owner.getId());
    }

    public void delete(Account account) {
        txRepo.deleteAll(txRepo.findAllByAccountId(account.getId()));
        cache.clearTxCache(account.getId());
        accountRepo.delete(account);
    }

    public Map<Long, Integer> updateAccountSort(Account account, AccountEndpoints.SortDirection direction) {
        List<Account> accounts = accountRepo.findAllByOwnerAndTypeOrderBySortOrder(account.getOwner(), account.getType());
        int index = accounts.indexOf(account);
        if ((direction == up && index == 0) || (direction == down && index == accounts.size() -1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort instruction");
        }
        Account affected = accounts.get(direction == up ? index - 1 : index + 1);
        int temp = accounts.get(index).getSortOrder();
        accounts.get(index).setSortOrder(affected.getSortOrder());
        affected.setSortOrder(temp);

        accounts.sort(Comparator.comparingInt(Account::getSortOrder));
        for (int i = 0; i < accounts.size(); i++) {
            accounts.get(i).setSortOrder(i);
        }
        accountRepo.saveAll(accounts);

        return accounts.stream().collect(Collectors.toMap(Account::getId, Account::getSortOrder));
    }
}
