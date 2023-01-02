package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.user.SledgerUser;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

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
    public void addCashTx() throws Exception {
        CashTransaction cashTx = CashTransaction.builder()
            .date(Instant.now())
            .account(Account.builder().id(1).build())
            .amount(BigDecimal.ONE)
            .remarks("Hello")
            .build();

        mvc.perform(request(POST, "/api/transaction", cashTx))
            .andExpect(status().isOk());
    }
}
