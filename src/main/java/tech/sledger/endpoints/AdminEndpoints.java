package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.service.AccountIssuerService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminEndpoints {
    private final AccountIssuerService accountIssuerService;

    public record NewAccountIssuer(String name) {}

    @PostMapping("/account-issuer")
    public AccountIssuer addAccountIssuer(@RequestBody NewAccountIssuer newAccountIssuer) {
        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName(newAccountIssuer.name.trim());
        return accountIssuerService.add(accountIssuer);
    }
}
