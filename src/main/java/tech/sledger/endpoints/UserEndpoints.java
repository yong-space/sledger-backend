package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.UserService;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserEndpoints {
    private final AccountIssuerService accountIssuerService;
    private final AccountService accountService;
    private final UserService userService;

    public record NewAccount(String name, AccountType type, long issuerId) {}

    @GetMapping("/account-issuer")
    public List<AccountIssuer> listAccountIssuers() {
        return accountIssuerService.list();
    }

    @PostMapping("/account")
    public Account addAccountIssuer(Authentication auth, @RequestBody NewAccount newAccount) {
        SledgerUser user = (SledgerUser) auth.getPrincipal();

        AccountIssuer issuer = accountIssuerService.get(newAccount.issuerId);
        if (issuer == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No such issuer");
        }

        Account account = Account.builder()
            .issuer(issuer)
            .name(newAccount.name)
            .type(newAccount.type)
            .owner(user)
            .build();
        return accountService.add(account);
    }

    @PutMapping("/account")
    public Account updateAccount(Authentication auth, @RequestBody Account account) {
        userService.authorise(auth, account);
        return accountService.save(account);
    }

    @DeleteMapping("/account/{accountId}")
    public void deleteAccount(Authentication auth, @PathVariable long accountId) {
        Account account = userService.authorise(auth, accountId);
        accountService.delete(account);
    }

    @GetMapping("/account")
    public List<Account> listAccounts(Authentication auth) {
        return accountService.list((SledgerUser) auth.getPrincipal());
    }
}
