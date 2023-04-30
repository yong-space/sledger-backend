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
import java.util.Map;
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
                .paymentAccountId(newAccount.getPaymentAccount())
                .paymentRemarks(newAccount.getPaymentRemarks())
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

    @PutMapping("/{accountId}")
    public Account updateAccount(Authentication auth, @PathVariable long accountId, @RequestBody NewAccount editAccount) {
        Account account = userService.authorise(auth, accountId);
        AccountIssuer issuer = accountIssuerService.get(editAccount.getIssuerId());
        if (issuer == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No such issuer");
        }

        switch (editAccount.getType()) {
            case Cash -> {
                var cashAccount = (CashAccount) account;
                cashAccount.setIssuer(issuer);
                cashAccount.setName(editAccount.getName());
                cashAccount.setMultiCurrency(editAccount.isMultiCurrency());
            }
            case Credit -> {
                var creditAccount = (CreditAccount) account;
                creditAccount.setIssuer(issuer);
                creditAccount.setName(editAccount.getName());
                creditAccount.setBillingCycle(editAccount.getBillingCycle());
                creditAccount.setPaymentAccountId(editAccount.getPaymentAccount());
                creditAccount.setPaymentRemarks(editAccount.getPaymentRemarks());
            }
            case Retirement -> {
                var cpfAccount = (CPFAccount) account;
                cpfAccount.setOrdinaryRatio(editAccount.getOrdinaryRatio());
                cpfAccount.setSpecialRatio(editAccount.getSpecialRatio());
                cpfAccount.setMedisaveRatio(editAccount.getMedisaveRatio());
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "Invalid account type");
        };
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
    public List<Map> listAccounts(Authentication auth) {
        return accountService.list((User) auth.getPrincipal());
    }
}
