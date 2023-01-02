package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.endpoints.UserEndpoints;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.repo.AccountIssuerRepo;
import tech.sledger.repo.AccountRepo;
import javax.annotation.PostConstruct;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class AccountTests extends BaseTest {
    @Autowired
    public UserConfig userConfig;
    @Autowired
    public AccountIssuerRepo accountIssuerRepo;
    @Autowired
    public AccountRepo accountRepo;

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
        UserEndpoints.NewAccount account = new UserEndpoints.NewAccount("a", AccountType.Cash, 123);
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("No such issuer"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addListDeleteAccount() throws Exception {
        UserEndpoints.NewAccount account = new UserEndpoints.NewAccount("a", AccountType.Cash, 1);
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk());
        mvc.perform(get("/api/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(delete("/api/account/1"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/account/2"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateAccount() throws Exception {
        UserEndpoints.NewAccount newAccount = new UserEndpoints.NewAccount("a", AccountType.Cash, 1);
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
