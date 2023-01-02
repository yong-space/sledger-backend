package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.user.SledgerUser;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class TransactionTests extends BaseTest {
    @PostConstruct
    public void init() {
        List<SledgerUser> users = userConfig.setupUsers();

        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer.setId(1);
        accountIssuerRepo.save(accountIssuer);

        Account cashAccount = Account.builder()
            .issuer(accountIssuer)
            .name("My Cash Account")
            .owner(users.get(0))
            .type(AccountType.Cash)
            .id(1)
            .build();
        accountRepo.save(cashAccount);
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
            .andExpect(status().reason("No such account id"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addDeleteCashTx() throws Exception {
        CashTransaction cashTx = CashTransaction.builder()
            .date(Instant.now())
            .account(Account.builder().id(1).build())
            .amount(BigDecimal.ONE)
            .remarks("Cash")
            .build();

        AtomicLong id = new AtomicLong();

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Cash"))
            .andDo(res -> id.set(objectMapper.readValue(res.getResponse().getContentAsString(), CashTransaction.class).getId()));

        mvc.perform(delete("/api/transaction/" + id))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addCreditAndListTx() throws Exception {
        CreditTransaction creditTx = CreditTransaction.builder()
            .date(Instant.now())
            .category("Shopping Test")
            .billingMonth(Instant.now())
            .account(Account.builder().id(1).build())
            .amount(BigDecimal.ONE)
            .remarks("Credit")
            .build();

        mvc.perform(request(POST, "/api/transaction", creditTx))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("Shopping Test"));

        mvc.perform(get("/api/transaction/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Shopping Test')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateTx() throws Exception {
        CashTransaction cashTx = CashTransaction.builder()
            .date(Instant.now())
            .account(Account.builder().id(1).build())
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
