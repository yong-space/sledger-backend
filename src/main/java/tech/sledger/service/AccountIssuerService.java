package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.repo.AccountIssuerRepo;
import tech.sledger.repo.AccountRepo;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AccountIssuerService {
    private final AccountIssuerRepo accountIssuerRepo;
    private final AccountRepo accountRepo;

    public AccountIssuer add(AccountIssuer accountIssuer) {
        AccountIssuer previous = accountIssuerRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        accountIssuer.setId(id);
        accountIssuer.setName(accountIssuer.getName().trim());
        return accountIssuerRepo.save(accountIssuer);
    }

    public AccountIssuer edit(AccountIssuer accountIssuer) {
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

    public void delete(AccountIssuer accountIssuer) {
        List<Account> existing = accountRepo.findAllByIssuer(accountIssuer);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "There are existing accounts under this issuer");
        }
        accountIssuerRepo.delete(accountIssuer);
    }
}
