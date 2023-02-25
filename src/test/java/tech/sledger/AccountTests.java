package tech.sledger;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.endpoints.AccountEndpoints;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class AccountTests extends BaseTest {
    private AccountIssuer accountIssuer;

    @PostConstruct
    public void init() {
        accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer = accountIssuerService.add(accountIssuer);
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addAccountBadIssuer() throws Exception {
        AccountEndpoints.NewAccount account = new AccountEndpoints.NewAccount("a", AccountType.Cash, 123, 0L);
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("No such issuer"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addListDeleteAccount() throws Exception {
        AccountEndpoints.NewAccount account = new AccountEndpoints.NewAccount("abc", AccountType.Cash, accountIssuer.getId(), 0L);
        AtomicLong id1 = new AtomicLong();
        AtomicLong id2 = new AtomicLong();
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk())
            .andDo(res -> id1.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));
        mvc.perform(request(POST, "/api/account", account))
            .andExpect(status().isOk())
            .andDo(res -> id2.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));
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
    public void deleteOtherOwnerAccount() throws Exception {
        Account account = accountService.add(accountService.add(Account.builder()
            .type(AccountType.Cash)
            .name("Hello")
            .issuer(accountIssuer)
            .owner(userService.get("basic-user2@company.com"))
            .build()));

        mvc.perform(delete("/api/account/" + account.getId()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("You are not the owner of this account"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateAccount() throws Exception {
        AtomicLong id = new AtomicLong();
        AccountEndpoints.NewAccount newAccount = new AccountEndpoints.NewAccount("a", AccountType.Cash, accountIssuer.getId(), 0L);
        mvc.perform(request(POST, "/api/account", newAccount))
            .andExpect(status().isOk())
            .andDo(res -> id.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));

        Account account = accountService.get(id.get());

        Map<String, Object> payload = Map.of(
            "@type", "cash",
            "id", account.getId(),
            "name", "a2",
            "type", account.getType(),
            "issuer", account.getIssuer(),
            "owner", account.getOwner().getId()
        );

        mvc.perform(request(PUT, "/api/account", payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("a2"));
        accountService.delete(account);
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void deleteMissingAccount() throws Exception {
        mvc.perform(delete("/api/account/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));
    }
}
