package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.sledger.model.SledgerUser;
import tech.sledger.model.account.Account;
import tech.sledger.repo.AccountRepo;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;

    public Account add(Account account) {
        Account previous = accountRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        account.setId(id);
        return accountRepo.save(account);
    }

    public List<Account> list(SledgerUser owner) {
        return accountRepo.findAllByOwnerOrderByName(owner);
    }

    public Account save(Account account) {
        return accountRepo.save(account);
    }

    public void delete(Account account) {
        accountRepo.delete(account);
    }

    public void deleteAll() {
        accountRepo.deleteAll();
    }
}
