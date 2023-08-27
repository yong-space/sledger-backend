package tech.sledger.endpoints;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.CPFAccount;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.account.NewAccount;
import tech.sledger.model.dto.AccountDTO;
import tech.sledger.model.user.User;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.CacheService;
import tech.sledger.service.UserService;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountEndpoints {
    private final AccountIssuerService accountIssuerService;
    private final AccountService accountService;
    private final UserService userService;
    private final CacheService cacheService;

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
                .multiCurrency(newAccount.isMultiCurrency())
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
    public Account updateAccount(
        Authentication auth, @PathVariable long accountId, @RequestBody NewAccount editAccount
    ) {
        Account account = userService.authorise(auth, accountId);
        AccountIssuer issuer = accountIssuerService.get(editAccount.getIssuerId());
        if (issuer == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No such issuer");
        }

        if (account instanceof CreditAccount creditAccount) {
            creditAccount.setIssuer(issuer);
            creditAccount.setName(editAccount.getName());
            creditAccount.setMultiCurrency(editAccount.isMultiCurrency());
            creditAccount.setBillingCycle(editAccount.getBillingCycle());
            creditAccount.setPaymentAccountId(editAccount.getPaymentAccount());
            creditAccount.setPaymentRemarks(editAccount.getPaymentRemarks());
        } else if (account instanceof CashAccount cashAccount) {
            cashAccount.setIssuer(issuer);
            cashAccount.setName(editAccount.getName());
            cashAccount.setMultiCurrency(editAccount.isMultiCurrency());
        } else {
            CPFAccount cpfAccount = (CPFAccount) account;
            cpfAccount.setOrdinaryRatio(editAccount.getOrdinaryRatio());
            cpfAccount.setSpecialRatio(editAccount.getSpecialRatio());
            cpfAccount.setMedisaveRatio(editAccount.getMedisaveRatio());
        }
        cacheService.clearAuthCache(accountId);
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
        cacheService.clearAuthCache(accountId);
    }

    @GetMapping
    public List<AccountDTO> listAccounts(Authentication auth) {
        return accountService.list((User) auth.getPrincipal());
    }
}
