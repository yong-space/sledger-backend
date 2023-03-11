package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.*;
import tech.sledger.model.user.User;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.UserService;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountEndpoints {
    private final AccountIssuerService accountIssuerService;
    private final AccountService accountService;
    private final UserService userService;

    public record NewAccount(String name, AccountType type, long issuerId, long billingCycle) {}

    @PostMapping
    public Account addAccount(Authentication auth, @RequestBody NewAccount newAccount) {
        User user = (User) auth.getPrincipal();

        AccountIssuer issuer = accountIssuerService.get(newAccount.issuerId);
        if (issuer == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No such issuer");
        }
        Account account = switch (newAccount.type) {
            case Cash -> Account.builder()
                .issuer(issuer)
                .name(newAccount.name)
                .type(newAccount.type)
                .owner(user)
                .build();
            case Credit -> CreditAccount.builder()
                .issuer(issuer)
                .name(newAccount.name)
                .type(newAccount.type)
                .billingCycle(newAccount.billingCycle)
                .owner(user)
                .build();
        };
        return accountService.add(account);
    }

    @PutMapping
    public Account updateAccount(Authentication auth, @RequestBody Account account) {
        userService.authorise(auth, account.getId());
        return accountService.edit(account);
    }

    @DeleteMapping("/{accountId}")
    public void deleteAccount(Authentication auth, @PathVariable long accountId) {
        Account account = userService.authorise(auth, accountId);
        accountService.delete(account);
    }

    @GetMapping
    public List<AccountDTO> listAccounts(Authentication auth) {
        return accountService.list((User) auth.getPrincipal());
    }
}
