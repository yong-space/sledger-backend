package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.SledgerUser;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.tx.CashTransaction;
import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebService {
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService txService;

    @GetMapping("/api/public/hello")
    public String hello() {
        userService.deleteAll();
        accountService.deleteAll();
        txService.deleteAll();

        SledgerUser user = userService.add(SledgerUser.builder()
            .username("abc@def.com")
            .password("hello")
            .build());

        Account cashAccount = accountService.add(Account.builder()
            .owner(user)
            .type(AccountType.Cash)
            .name("Meh Meh Account")
            .build());

        CashTransaction cashTx = txService.add(CashTransaction.builder()
            .date(Instant.now())
            .account(cashAccount)
            .amount(BigDecimal.valueOf(200L))
            .category("Meh Meh")
            .remarks("Hello World")
            .build());

        return "Hello";
    }
}
