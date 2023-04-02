package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.service.AccountIssuerService;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminEndpoints {
    private final AccountIssuerService accountIssuerService;

    public record NewAccountIssuer(String name) {}

    @PostMapping("/account-issuer")
    public AccountIssuer addAccountIssuer(@RequestBody AccountIssuer accountIssuer) {
        return accountIssuerService.add(accountIssuer);
    }

    @PutMapping("/account-issuer")
    public AccountIssuer updateAccountIssuer(@RequestBody AccountIssuer accountIssuer) {
        return accountIssuerService.edit(accountIssuer);
    }

    @DeleteMapping("/account-issuer/{accountIssuerId}")
    public void deleteAccountIssuer(@PathVariable long accountIssuerId) {
        AccountIssuer accountIssuer = accountIssuerService.get(accountIssuerId);
        if (accountIssuer == null) {
            throw new ResponseStatusException(NOT_FOUND, "No such account issuer id");
        }
        accountIssuerService.delete(accountIssuer);
    }
}
