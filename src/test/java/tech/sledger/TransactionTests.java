package tech.sledger;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CPFAccount;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionTests extends BaseTest {
    private static long cashAccountId;
    private static long creditAccountId;
    private static long cpfAccountId;
    private static Integer cashId1;
    private static Integer cashId2;
    private static Integer creditId1;
    private static Integer creditId2;
    private static Integer cpfId;
    private static Map<String, Object> cashPayload;

    @BeforeAll
    public void init() {
        txRepo.deleteAll();
        accountRepo.deleteAll();
    }

    @Test
    @Order(1)
    public void setup() {
        AccountIssuer accountIssuerA = new AccountIssuer();
        accountIssuerA.setName("a");
        accountIssuerA = accountIssuerService.add(accountIssuerA);

        User user = userService.get("basic-user@company.com");

        cashAccountId = accountService.add(CashAccount.builder()
            .issuer(accountIssuerA)
            .name("My Cash Account")
            .owner(user)
            .multiCurrency(false)
            .type(AccountType.Cash)
            .build()).getId();

        creditAccountId = accountService.add(CreditAccount.builder()
            .issuer(accountIssuerA)
            .name("My Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .billingMonthOffset(0)
            .multiCurrency(false)
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

        cashPayload = new HashMap<>(Map.of(
            "@type", "cash",
            "date", date("2022-01-01"),
            "category", "Cash Category",
            "subCategory", "Cash Sub-category",
            "accountId", cashAccountId,
            "amount", 1,
            "remarks", "Super cali fragile"
        ));
    }

    @Test
    @Order(2)
    @WithUserDetails("basic-user@company.com")
    public void addTxBadInput() throws Exception {
        Map<String, Object> payload = new HashMap<>(Map.of(
            "@type", "cash",
            "date", Instant.now(),
            "accountId", 1234,
            "amount", 1,
            "remarks", "Hello"
        ));

        mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));

        Map<String, Object> payload2 = new HashMap<>(payload);
        payload2.put("accountId", cashAccountId);
        Map<String, Object> payload3 = new HashMap<>(payload);
        payload3.put("accountId", creditAccountId);
        payload3.put("@type", "credit");

        mvc.perform(request(POST, "/api/transaction", List.of(payload2, payload3)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Bulk operations can only be performed on transactions under the same account"));
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void addCashTx() throws Exception {
        String result1 = mvc.perform(request(POST, "/api/transaction", List.of(cashPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Super cali fragile"))
            .andReturn().getResponse().getContentAsString();
        cashId1 = JsonPath.parse(result1).read("$.[0].id");

        String result2 = mvc.perform(request(POST, "/api/transaction", List.of(cashPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].date").value("2022-01-01T00:00:01Z"))
            .andExpect(jsonPath("$.[0].balance").value(BigDecimal.valueOf(2)))
            .andReturn().getResponse().getContentAsString();
        cashId2 = JsonPath.parse(result2).read("$.[0].id");
    }

    @Test
    @Order(4)
    @WithUserDetails("basic-user@company.com")
    public void addCreditTx() throws Exception {
        Map<String, Object> payload = Map.of(
            "@type", "credit",
            "date", "2024-01-01T00:00:00.000Z",
            "category", "Credit Category",
            "billingMonth", Instant.now(),
            "accountId", creditAccountId,
            "amount", 1,
            "remarks", "cali"
        );

        String result1 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].category").value("Credit Category"))
            .andReturn().getResponse().getContentAsString();
        creditId1 = JsonPath.parse(result1).read("$.[0].id");

        String result2 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].category").value("Credit Category"))
            .andReturn().getResponse().getContentAsString();
        creditId2 = JsonPath.parse(result2).read("$.[0].id");
    }

    @Test
    @Order(5)
    @WithUserDetails("basic-user@company.com")
    public void addCpfTx() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("@type", "retirement");
        payload.put("accountId", cpfAccountId);
        payload.put("code", "CON");
        payload.put("company", "ABC Pte Ltd");
        payload.put("date", date("2022-01-01"));
        payload.put("forMonth", date("2022-01-01"));
        payload.put("amount", 1000);
        payload.put("ordinaryAmount", 400);
        payload.put("specialAmount", 300);
        payload.put("medisaveAmount", 300);

        String result = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].ordinaryAmount").value(400))
            .andReturn().getResponse().getContentAsString();
        cpfId = JsonPath.parse(result).read("$.[0].id");
    }

    @Test
    @Order(6)
    @WithUserDetails("basic-user@company.com")
    public void listTx() throws Exception {
        mvc.perform(get("/api/transaction/" + cashAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Cash Category')]").exists());

        mvc.perform(get("/api/transaction/" + creditAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Credit Category')]").exists());

        mvc.perform(get("/api/transaction/0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Cash Category')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Credit Category')]").exists());

        mvc.perform(get("/api/transaction/0?q=ca"))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/api/transaction/0?category=abc"))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/api/transaction/0?subCategory=abc"))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/api/transaction/0?q=cali"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)));

        mvc.perform(get("/api/transaction/0?category=Credit Category&from=2024-01-01T00:00:00.000Z&to=2024-01-02T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/transaction/0?subCategory=Cash Sub-category&from=2022-01-01T00:00:00.000Z&to=2022-01-02T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/transaction/0?from=2022-01-01T00:00:00.000Z&to=2022-01-02T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/transaction/0?from=2022-01-01T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)));

        mvc.perform(get("/api/transaction/0?to=2022-01-01T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)));

        mvc.perform(get("/api/transaction/" + cashAccountId + "?from=2022-01-01T00:00:00.000Z&to=2022-01-02T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/transaction/" + cashAccountId + "?from=2022-01-01T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/transaction/" + cashAccountId + "?to=2022-01-01T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Order(7)
    @WithUserDetails("basic-user@company.com")
    public void calculateCashBalanceAndDates() throws Exception {
        Map<String, Object> payload2 = new HashMap<>(cashPayload);
        payload2.put("date", date("2021-05-28"));
        mvc.perform(request(POST, "/api/transaction", List.of(payload2)))
            .andExpect(status().isOk());

        Map<String, Object> payload3 = new HashMap<>(cashPayload);
        payload3.put("date", date("2023-05-28"));
        mvc.perform(request(POST, "/api/transaction", List.of(payload3)))
            .andExpect(status().isOk());

        payload2.put("date", date("2023-05-22"));
        mvc.perform(request(POST, "/api/transaction", List.of(cashPayload, payload2, payload3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].date").value("2022-01-01T00:00:02Z"))
            .andExpect(jsonPath("$.[1].balance").value(BigDecimal.valueOf(5)));
    }

    @Test
    @Order(8)
    @WithUserDetails("basic-user@company.com")
    public void suggest() throws Exception {
        mvc.perform(get("/api/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.id == " + cashAccountId + ")].transactions").value(7));

        mvc.perform(get("/api/suggest/remarks?q=agile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("Super cali fragile")));

        mvc.perform(get("/api/suggest/code?q=co"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("CON")));

        mvc.perform(get("/api/suggest/company?q=abc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("ABC Pte Ltd")));

        mvc.perform(get("/api/suggest/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == \"Cash Category\")].subCategory").value("Cash Sub-category"));
    }

    @Test
    @Order(9)
    @WithUserDetails("basic-user@company.com")
    public void updateTx() throws Exception {
        Map<String, Object> payload = Map.of(
            "@type", "cash",
            "date", date("2023-01-01"),
            "category", "Edit Test",
            "accountId", cashAccountId,
            "amount", 1,
            "remarks", "Cash"
        );
        String result1 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Cash"))
            .andReturn().getResponse().getContentAsString();
        String result2 = mvc.perform(request(POST, "/api/transaction", List.of(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Cash"))
            .andReturn().getResponse().getContentAsString();
        Integer id1 = JsonPath.parse(result1).read("$.[0].id");
        Integer id2 = JsonPath.parse(result2).read("$.[0].id");

        Map<String, Object> payload2 = new HashMap<>(payload);
        payload2.put("date", date("2022-01-01"));
        payload2.put("remarks", "Edited");
        payload2.put("id", id1);

        Map<String, Object> payload3 = new HashMap<>(payload2);
        payload3.put("date", date("2020-01-01"));
        payload3.put("id", id2);
        payload3.put("amount", 2);

        mvc.perform(request(PUT, "/api/transaction", List.of(payload2, payload3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Edited"));

        Map<String, Object> payload4 = new HashMap<>(payload2);
        payload4.put("remarks", "Modified");

        mvc.perform(request(PUT, "/api/transaction", List.of(payload4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Modified"));
    }

    @Test
    @Order(10)
    @WithUserDetails("basic-user@company.com")
    public void bulkUpdateTx() throws Exception {
        Map<String, Object> payload1 = Map.of(
            "ids", List.of(cashId1, cashId2),
            "remarks", "Bulk Remarks",
            "category", "Bulk Category",
            "subCategory", "Bulk Sub-category"
        );
        mvc.perform(request(PUT, "/api/transaction/bulk", Map.of("ids", List.of(123))))
            .andExpect(status().isBadRequest());

        mvc.perform(request(PUT, "/api/transaction/bulk", payload1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Bulk Remarks"));

        mvc.perform(get("/api/transaction/" + cashAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.category == 'Bulk Category')]", hasSize(2)));

        mvc.perform(request(PUT, "/api/transaction/bulk", Map.of("ids", List.of(cashId1, cashId2))))
            .andExpect(status().isOk());

        Map<String, Object> payload2 = Map.of(
            "ids", List.of(creditId1, creditId2),
            "remarks", "Bulk Remarks",
            "category", "Bulk Category",
            "subCategory", "Bulk Sub-category",
            "billingMonth", "2023-01-01T00:00:00.000Z"
        );
        mvc.perform(request(PUT, "/api/transaction/bulk", payload2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].remarks").value("Bulk Remarks"));

        mvc.perform(request(PUT, "/api/transaction/bulk", Map.of("ids", List.of(creditId1, creditId2))))
            .andExpect(status().isOk());

        Map<String, Object> payload4 = Map.of("ids", List.of(cpfId));
        mvc.perform(request(PUT, "/api/transaction/bulk", payload4))
            .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    @WithUserDetails("basic-user@company.com")
    public void editTxDateOnly() throws Exception {
        // Use dates in year 2000, before all other test data, so the epoch is 0
        // and balances are deterministic: A=101, B=152, C=178
        // Amounts chosen to avoid scientific-notation serialization (e.g. 100 -> 1E+2)
        Map<String, Object> txA = new HashMap<>(Map.of(
            "@type", "cash", "accountId", cashAccountId,
            "date", date("2000-01-01"), "amount", 101, "remarks", "DateEditA"
        ));
        Map<String, Object> txB = new HashMap<>(Map.of(
            "@type", "cash", "accountId", cashAccountId,
            "date", date("2000-02-01"), "amount", 51, "remarks", "DateEditB"
        ));
        Map<String, Object> txC = new HashMap<>(Map.of(
            "@type", "cash", "accountId", cashAccountId,
            "date", date("2000-03-01"), "amount", 26, "remarks", "DateEditC"
        ));
        String rA = mvc.perform(request(POST, "/api/transaction", List.of(txA))).andReturn().getResponse().getContentAsString();
        String rB = mvc.perform(request(POST, "/api/transaction", List.of(txB))).andReturn().getResponse().getContentAsString();
        String rC = mvc.perform(request(POST, "/api/transaction", List.of(txC))).andReturn().getResponse().getContentAsString();
        int idA = JsonPath.parse(rA).read("$.[0].id");
        int idB = JsonPath.parse(rB).read("$.[0].id");
        int idC = JsonPath.parse(rC).read("$.[0].id");

        // Scenario 1: date-only backward move — C from 2000-03-01 to 2000-01-15 (between A and B).
        // Previously this skipped recalculation entirely, leaving B with a stale balance.
        // Expected order/balances: A(101), C(127), B(178)
        Map<String, Object> editC = new HashMap<>(Map.of(
            "@type", "cash", "accountId", cashAccountId,
            "id", idC, "date", date("2000-01-15"), "amount", 26, "remarks", "DateEditC"
        ));
        mvc.perform(request(PUT, "/api/transaction", List.of(editC)))
            .andExpect(status().isOk());

        // Verify order and balances using index-based access (filter expressions have numeric type issues)
        mvc.perform(get("/api/transaction/" + cashAccountId + "?from=2000-01-01T00:00:00.000Z&to=2000-03-31T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$.[0].remarks").value("DateEditA"))
            .andExpect(jsonPath("$.[0].balance").value(BigDecimal.valueOf(101)))
            .andExpect(jsonPath("$.[1].remarks").value("DateEditC"))
            .andExpect(jsonPath("$.[1].balance").value(BigDecimal.valueOf(127)))
            .andExpect(jsonPath("$.[2].remarks").value("DateEditB"))
            .andExpect(jsonPath("$.[2].balance").value(BigDecimal.valueOf(178)));

        // Scenario 2: date-only forward move — A from 2000-01-01 to 2000-01-20 (past C).
        // Previously used the new date as minDate, so C (between old and new positions) was not recomputed.
        // Expected order/balances: C(26), A(127), B(178)
        Map<String, Object> editA = new HashMap<>(Map.of(
            "@type", "cash", "accountId", cashAccountId,
            "id", idA, "date", date("2000-01-20"), "amount", 101, "remarks", "DateEditA"
        ));
        mvc.perform(request(PUT, "/api/transaction", List.of(editA)))
            .andExpect(status().isOk());

        mvc.perform(get("/api/transaction/" + cashAccountId + "?from=2000-01-01T00:00:00.000Z&to=2000-03-31T00:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$.[0].remarks").value("DateEditC"))
            .andExpect(jsonPath("$.[0].balance").value(BigDecimal.valueOf(26)))
            .andExpect(jsonPath("$.[1].remarks").value("DateEditA"))
            .andExpect(jsonPath("$.[1].balance").value(BigDecimal.valueOf(127)))
            .andExpect(jsonPath("$.[2].remarks").value("DateEditB"))
            .andExpect(jsonPath("$.[2].balance").value(BigDecimal.valueOf(178)));
    }

    @Test
    @Order(12)
    @WithUserDetails("basic-user@company.com")
    public void deleteCashTx() throws Exception {
        mvc.perform(delete("/api/transaction/" + cashId1))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + cashId2))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/transaction/" + cashId1))
            .andExpect(status().isBadRequest());
    }
}
