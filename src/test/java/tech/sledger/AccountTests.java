package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.endpoints.AccountEndpoints;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class AccountTests extends BaseTest {
    @PostConstruct
    public void init() {
        userConfig.setupUsers();

        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer.setId(1);
        accountIssuerRepo.save(accountIssuer);
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addAccountBadIssuer() throws Exception {
        AccountEndpoints.NewAccount account = new AccountEndpoints.NewAccount("a", AccountType.Cash, 123);
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("No such issuer"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addListDeleteAccount() throws Exception {
        AccountEndpoints.NewAccount account = new AccountEndpoints.NewAccount("abc", AccountType.Cash, 1);
        AtomicLong id1 = new AtomicLong();
        AtomicLong id2 = new AtomicLong();
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk())
            .andDo(res -> id1.set(objectMapper.readValue(res.getResponse().getContentAsString(), Account.class).getId()));
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk())
            .andDo(res -> id2.set(objectMapper.readValue(res.getResponse().getContentAsString(), Account.class).getId()));
        mvc.perform(get("/api/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.id == " + id1.get() + ")]").exists())
            .andExpect(jsonPath("$.[?(@.id == " + id2.get() + ")]").exists());
        mvc.perform(delete("/api/account/" + id1.get()))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/account/" + id2.get()))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateAccount() throws Exception {
        AccountEndpoints.NewAccount newAccount = new AccountEndpoints.NewAccount("a", AccountType.Cash, 1);
        mvc.perform(request(POST, "/api/account", newAccount))
            .andExpect(status().isOk());
        Account account = accountRepo.findById(1L).orElse(null);
        assert account != null;
        account.setName("a2");
        mvc.perform(request(PUT, "/api/account", account))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("a2"));
        accountRepo.deleteById(1L);
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void deleteMissingAccount() throws Exception {
        mvc.perform(delete("/api/account/99999"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("No such account id"));
    }
}
