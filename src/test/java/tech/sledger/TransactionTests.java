package tech.sledger;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;
import com.jayway.jsonpath.JsonPath;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CPFAccount;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionTests extends BaseTest {
    private long cashAccountId;
    private long cpfAccountId;

    @PostConstruct
    public void init() {
        AccountIssuer accountIssuerA = new AccountIssuer();
        accountIssuerA.setName("a");
        accountIssuerA = accountIssuerService.add(accountIssuerA);

        User user = userService.get("basic-user@company.com");

        cashAccountId = accountService.add(CashAccount.builder()
            .issuer(accountIssuerA)
            .name("My Cash Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();

        cpfAccountId = accountService.add(CPFAccount.builder()
            .issuer(accountIssuerA)
            .name("CPF")
            .owner(user)
            .type(AccountType.Retirement)
            .ordinaryRatio(BigDecimal.valueOf(0.4))
            .specialRatio(BigDecimal.valueOf(0.3))
            .medisaveRatio(BigDecimal.valueOf(0.3))
            .build()).getId();
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

        mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addDeleteCashTx() throws Exception {
        Instant date = Instant.ofEpochMilli(1640995200000L);
        Map<String, Object> payload = new HashMap<>(Map.of(
            "@type", "cash",
            "date", date,
            "category", "Shopping Test",
            "accountId", cashAccountId,
            "amount", 1,
            "remarks", "Super cali fragile"
        ));

        String result1 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Super cali fragile"))
            .andReturn().getResponse().getContentAsString();
        Integer id1 = JsonPath.parse(result1).read("$.[0].id");

        String result2 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].date").value("2022-01-01T00:00:01Z"))
            .andExpect(jsonPath("$.[0].balance").value(BigDecimal.valueOf(2)))
            .andReturn().getResponse().getContentAsString();
        Integer id2 = JsonPath.parse(result2).read("$.[0].id");

        Map<String, Object> payload2 = new HashMap<>(payload);
        payload2.put("date", Instant.ofEpochMilli(1622195095000L));
        mvc.perform(request(POST, "/api/transaction", List.of(payload2))).andExpect(status().isOk());
        Map<String, Object> payload3 = new HashMap<>(payload);
        payload3.put("date", Instant.ofEpochMilli(1685266523000L));
        mvc.perform(request(POST, "/api/transaction", List.of(payload3))).andExpect(status().isOk());

        payload.put("account", Map.of("id", cashAccountId));
        List.of(payload2, payload3).forEach(p -> p.put("account", cashAccountId));
        payload2.put("date", Instant.ofEpochMilli(1684770153000L));
        mvc.perform(request(POST, "/api/transaction", List.of(payload, payload2, payload3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].date").value("2022-01-01T00:00:02Z"))
            .andExpect(jsonPath("$.[1].balance").value(BigDecimal.valueOf(5)));

        mvc.perform(get("/api/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.id == " + cashAccountId + ")].transactions").value(7));

        mvc.perform(get("/api/suggest/remarks?q=agile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("Super cali fragile")));

        mvc.perform(get("/api/suggest/category?q=hopping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("Shopping Test")));

        mvc.perform(delete("/api/transaction/" + id1))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + id2))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + id1))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addCpfTx() throws Exception {
        Instant date = Instant.ofEpochMilli(1640995200000L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("@type", "retirement");
        payload.put("accountId", cpfAccountId);
        payload.put("code", "CON");
        payload.put("company", "ABC Pte Ltd");
        payload.put("date", date);
        payload.put("forMonth", date);
        payload.put("amount", 1000);
        payload.put("ordinaryAmount", 400);
        payload.put("specialAmount", 300);
        payload.put("medisaveAmount", 300);

        mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].ordinaryAmount").value(400));

        mvc.perform(get("/api/suggest/code?q=co"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("CON")));

        mvc.perform(get("/api/suggest/company?q=abc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("ABC Pte Ltd")));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addCreditAndListTx() throws Exception {
        Map<String, Object> payload = Map.of(
            "@type", "credit",
            "date", Instant.now(),
            "category", "Shopping Test",
            "billingMonth", Instant.now(),
            "accountId", cashAccountId,
            "amount", 1,
            "remarks", "Credit"
        );

        mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].category").value("Shopping Test"));

        mvc.perform(get("/api/transaction/" + cashAccountId))
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
            "accountId", cashAccountId,
            "amount", 1,
            "remarks", "Cash"
        );
        String result = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Cash"))
            .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.parse(result).read("$.[0].id");

        Map<String, Object> payload2 = new HashMap<>(payload);
        payload2.put("remarks", "Edited");
        payload2.put("id", id);

        mvc.perform(request(PUT, "/api/transaction", List.of(payload2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Edited"));
    }
}
