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

    @PostMapping
    public Account addAccount(Authentication auth, @RequestBody NewAccount newAccount) {
        User user = (User) auth.getPrincipal();

        AccountIssuer issuer = accountIssuerService.get(newAccount.getIssuerId());
        if (issuer == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No such issuer");
        }
        Account account = switch(newAccount.getType()) {
            case Cash -> CashAccount.builder()
                .issuer(issuer)
                .name(newAccount.getName())
                .type(newAccount.getType())
                .owner(user)
                .multiCurrency(newAccount.isMultiCurrency())
                .build();
            case Credit -> CreditAccount.builder()
                .issuer(issuer)
                .name(newAccount.getName())
                .type(newAccount.getType())
                .billingCycle(newAccount.getBillingCycle())
                .owner(user)
                .build();
            case Retirement -> CPFAccount.builder()
                .issuer(issuer)
                .name("CPF")
                .type(newAccount.getType())
                .owner(user)
                .ordinaryRatio(newAccount.getOrdinaryRatio())
                .specialRatio(newAccount.getSpecialRatio())
                .medisaveRatio(newAccount.getMedisaveRatio())
                .build();
            default -> null;
        };
        if (account == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid new account type");
        }
        return accountService.add(account);
    }

    @PutMapping
    public Account updateAccount(Authentication auth, @RequestBody Account account) {
        userService.authorise(auth, account.getId());
        return accountService.edit(account);
    }

    @PutMapping("/{accountId}/{visible}")
    public Account updateAccountVisibility(Authentication auth, @PathVariable long accountId, @PathVariable boolean visible) {
        Account account = accountService.get(accountId);
        userService.authorise(auth, account.getId());
        account.setVisible(visible);
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
