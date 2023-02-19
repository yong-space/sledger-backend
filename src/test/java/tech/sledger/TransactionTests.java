package tech.sledger;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.service.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
        Map<String, Object> payload = Map.of(
            "@type", "cash",
            "date", Instant.now(),
            "account", Map.of("id", 1234),
            "amount", 1,
            "remarks", "Hello"
        );

        mvc.perform(request(POST, "/api/transaction", payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addDeleteCashTx() throws Exception {
        Instant date = Instant.ofEpochMilli(1640995200000L);
        Map<String, Object> payload = Map.of(
            "@type", "cash",
            "date", date,
            "category", "Shopping Test",
            "account", Map.of("id", accountId),
            "amount", 1,
            "remarks", "Cash"
        );

        AtomicLong id1 = new AtomicLong();
        AtomicLong id2 = new AtomicLong();

        mvc.perform(request(POST, "/api/transaction", payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Cash"))
            .andDo(res -> id1.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));

        mvc.perform(request(POST, "/api/transaction", payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value("2022-01-01T00:00:01Z"))
            .andExpect(jsonPath("$.balance").value(BigDecimal.valueOf(2)))
            .andDo(res -> id2.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));

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
        Map<String, Object> payload = Map.of(
            "@type", "credit",
            "date", Instant.now(),
            "category", "Shopping Test",
            "billingMonth", Instant.now(),
            "account", Map.of("id", accountId),
            "amount", 1,
            "remarks", "Credit"
        );

        mvc.perform(request(POST, "/api/transaction", payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("Shopping Test"));

        mvc.perform(get("/api/transaction/" + accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Shopping Test')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateTx() throws Exception {
        Map<String, Object> payload = Map.of(
            "@type", "cash",
            "date", Instant.now(),
            "category", "Shopping Test",
            "account", Map.of("id", accountId),
            "amount", 1,
            "remarks", "Cash"
        );

        mvc.perform(request(POST, "/api/transaction", payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Cash"));

        Map<String, Object> payload2 = new HashMap<>(payload);
        payload2.put("remarks", "Edited");

        mvc.perform(request(PUT, "/api/transaction", payload2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remarks").value("Edited"));
    }
}
