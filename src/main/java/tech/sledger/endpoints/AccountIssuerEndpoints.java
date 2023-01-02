package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.service.AccountIssuerService;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account-issuer")
public class AccountIssuerEndpoints {
    private final AccountIssuerService accountIssuerService;

    @GetMapping
    public List<AccountIssuer> listAccountIssuers() {
        return accountIssuerService.list();
    }
}
