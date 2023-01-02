package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.repo.AccountIssuerRepo;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountIssuerService {
    private final AccountIssuerRepo accountIssuerRepo;

    public AccountIssuer add(AccountIssuer accountIssuer) {
        AccountIssuer previous = accountIssuerRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        accountIssuer.setId(id);
        return accountIssuerRepo.save(accountIssuer);
    }

    public AccountIssuer get(String name) {
        return accountIssuerRepo.findFirstByName(name);
    }

    public AccountIssuer get(long id) {
        return accountIssuerRepo.findById(id).orElse(null);
    }

    public List<AccountIssuer> list() {
        return accountIssuerRepo.findAll();
    }

    public AccountIssuer save(AccountIssuer accountIssuer) {
        return accountIssuerRepo.save(accountIssuer);
    }

    public void delete(AccountIssuer accountIssuer) {
        accountIssuerRepo.delete(accountIssuer);
    }
}
