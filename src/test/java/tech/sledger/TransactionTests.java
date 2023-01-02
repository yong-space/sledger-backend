package tech.sledger;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.service.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class TransactionTests extends BaseTest {
    @Autowired
    private UserService userService;
    private long accountId;

    @PostConstruct
    public void init() {
        AccountIssuer accountIssuerA = new AccountIssuer();
        accountIssuerA.setName("a");
        accountIssuerA = accountIssuerService.add(accountIssuerA);

        Account cashAccount = Account.builder()
            .issuer(accountIssuerA)
            .name("My Cash Account")
            .owner(userService.get("basic-user@company.com"))
            .type(AccountType.Cash)
            .build();
        accountId = accountService.add(cashAccount).getId();
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addTxBadAccount() throws Exception {
        CashTransaction cashTx = CashTransaction.builder()
            .date(Instant.now())
            .account(Account.builder().id(1234).build())
            .amount(BigDecimal.ONE)
            .remarks("Hello")
            .build();

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addDeleteCashTx() throws Exception {
        Instant date = Instant.ofEpochMilli(1640995200000L);
        CashTransaction cashTx = CashTransaction.builder()
            .date(date)
            .account(Account.builder().id(accountId).build())
            .amount(BigDecimal.ONE)
            .remarks("Cash")
            .build();

        AtomicLong id1 = new AtomicLong();
        AtomicLong id2 = new AtomicLong();

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Cash"))
            .andDo(res -> id1.set(objectMapper.readValue(res.getResponse().getContentAsString(), CashTransaction.class).getId()));

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value("2022-01-01T00:00:01Z"))
            .andExpect(jsonPath("$.balance").value(BigDecimal.valueOf(2)))
            .andDo(res -> id2.set(objectMapper.readValue(res.getResponse().getContentAsString(), CashTransaction.class).getId()));

        mvc.perform(delete("/api/transaction/" + id1))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + id2))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + id1))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addCreditAndListTx() throws Exception {
        CreditTransaction creditTx = CreditTransaction.builder()
            .date(Instant.now())
            .category("Shopping Test")
            .billingMonth(Instant.now())
            .account(Account.builder().id(accountId).build())
            .amount(BigDecimal.ONE)
            .remarks("Credit")
            .build();

        mvc.perform(request(POST, "/api/transaction", creditTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("Shopping Test"));

        mvc.perform(get("/api/transaction/" + accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Shopping Test')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateTx() throws Exception {
        CashTransaction cashTx = CashTransaction.builder()
            .date(Instant.now())
            .account(Account.builder().id(accountId).build())
            .amount(BigDecimal.ONE)
            .remarks("Cash")
            .build();

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Cash"));

        cashTx.setRemarks("Edited");

        mvc.perform(request(PUT, "/api/transaction", cashTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Edited"));
    }
}
