package tech.sledger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CashAccount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountTests extends BaseTest {
    private static AccountIssuer accountIssuer;
    private static List<Map<String, Object>> newAccounts;

    @Test
    @Order(1)
    @WithUserDetails("basic-user@company.com")
    public void setup() {
        accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer = accountIssuerService.add(accountIssuer);

        newAccounts = List.of(
            new HashMap<>(Map.of(
                "name", "cashAccount",
                "type", "Cash",
                "issuerId", accountIssuer.getId(),
                "multiCurrency", false
            )),
            new HashMap<>(Map.of(
                "name", "creditAccount",
                "type", "Credit",
                "issuerId", accountIssuer.getId(),
                "billingCycle", 15L
            )),
            new HashMap<>(Map.of(
                "name", "creditAccountNoPayment",
                "type", "Credit",
                "issuerId", accountIssuer.getId(),
                "billingCycle", 1L
            )),
            new HashMap<>(Map.of(
                "type", "Retirement",
                "issuerId", accountIssuer.getId(),
                "ordinaryRatio", 0.5677,
                "specialRatio", 0.1891,
                "medisaveRatio", 0.2432
            ))
        );
    }

    @Test
    @Order(2)
    @WithUserDetails("basic-user@company.com")
    public void addAccounts() throws Exception {
        for (Map<String, Object> account : newAccounts) {
            MvcResult result = mvc.perform(request(POST, "/api/account", account))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(account.get("type")))
                .andReturn();
            account.put("id", JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        }

        mvc.perform(get("/api/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.id == " + newAccounts.get(0).get("id") + ")]").exists())
            .andExpect(jsonPath("$.[?(@.id == " + newAccounts.get(1).get("id") + ")]").exists())
            .andExpect(jsonPath("$.[?(@.id == " + newAccounts.get(2).get("id") + ")]").exists())
            .andExpect(jsonPath("$.[?(@.id == " + newAccounts.get(3).get("id") + ")]").exists());
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void updateAccounts() throws Exception {
        for (Map<String, Object> account : newAccounts) {
            mvc.perform(put("/api/account/" + account.get("id") + "?visible=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

            mvc.perform(request(PUT, "/api/account/" + account.get("id"), account))
                .andExpect(status().isOk());
        }

        String id = newAccounts.get(1).get("id").toString();
        mvc.perform(put("/api/account/" + id + "/sort/up"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/" + id + "/sort/down"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + id).value(1));
        mvc.perform(put("/api/account/" + id + "/sort/down"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/" + id + "/sort/up"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + id).value(0));
    }

    @Test
    @Order(4)
    @WithUserDetails("basic-user@company.com")
    public void badInput() throws Exception {
        Map<String, ?> badIssuerAccount = Map.of(
            "name", "cashAccount",
            "type", "Cash",
            "issuerId", 678934789,
            "multiCurrency", false
        );
        mvc.perform(request(POST, "/api/account", badIssuerAccount))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("No such issuer"));

        var badEditAccount = newAccounts.get(0);
        badEditAccount.put("issuerId", 678934789);
        mvc.perform(request(PUT, "/api/account/" + badEditAccount.get("id"), badEditAccount))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("No such issuer"));

        Map<String, Object> noIssuerAccount = Map.of("name", "badAccount");
        mvc.perform(request(POST, "/api/account", noIssuerAccount))
            .andExpect(status().isBadRequest());

        Map<String, Object> otherTypeAccount = Map.of(
            "name", "otherAccount",
            "type", "Other",
            "issuerId", accountIssuer.getId()
        );
        mvc.perform(request(POST, "/api/account", otherTypeAccount))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid new account type"));
    }

    @Test
    @Order(5)
    @WithUserDetails("basic-user@company.com")
    public void deleteAccounts() throws Exception {
        for (Map<String, Object> account : newAccounts) {
            mvc.perform(delete("/api/account/" + account.get("id")))
                .andExpect(status().isOk());
        }
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void deleteOtherOwnerAccount() throws Exception {
        Account account = accountService.add(accountService.add(CashAccount.builder()
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
    public void deleteMissingAccount() throws Exception {
        mvc.perform(delete("/api/account/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No such account id"));
    }
}
