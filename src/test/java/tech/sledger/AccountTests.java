package tech.sledger;

import org.junit.jupiter.api.*;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CashAccount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountTests extends BaseTest {
    private static AccountIssuer accountIssuer;
    private static Account someoneElsesAccount;
    private static List<Map<String, Object>> newAccounts;

    @Test
    @Order(1)
    @WithUserDetails("basic-user@company.com")
    public void setup() {
        accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer = accountIssuerService.add(accountIssuer);

        Account myAccount = accountService.add(CashAccount.builder()
            .issuer(accountIssuer)
            .name("My Account")
            .type(AccountType.Cash)
            .owner(userService.get("basic-user@company.com"))
            .build());

        someoneElsesAccount = accountService.add(CashAccount.builder()
            .issuer(accountIssuer)
            .name("Someone Else's Account")
            .type(AccountType.Cash)
            .owner(userService.get("admin-user@company.com"))
            .build());

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
                "billingCycle", 15L,
                "paymentAccount", myAccount.getId()
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
            AtomicLong id = new AtomicLong();
            mvc.perform(request(POST, "/api/account", account))
                .andExpect(status().isOk())
                .andDo(res -> id.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), Map.class).get("id")));
            account.put("id", id.get());
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

        mvc.perform(put("/api/account/" + newAccounts.get(0).get("id") + "/sort/down"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/" + newAccounts.get(0).get("id") + "/sort/up"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + newAccounts.get(0).get("id")).value(0));
        mvc.perform(put("/api/account/" + newAccounts.get(0).get("id") + "/sort/up"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/" + newAccounts.get(0).get("id") + "/sort/down"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + newAccounts.get(0).get("id")).value(1));
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

        Map<String, ?> badPaymentAccount = Map.of(
            "name", "creditAccount",
            "type", "Credit",
            "issuerId", accountIssuer.getId(),
            "paymentAccount", 1234567
        );
        mvc.perform(request(POST, "/api/account", badPaymentAccount))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid payment account id"));

        Map<String, ?> badPaymentAccount2 = Map.of(
            "name", "creditAccount",
            "type", "Credit",
            "issuerId", accountIssuer.getId(),
            "paymentAccount", someoneElsesAccount.getId()
        );
        mvc.perform(request(POST, "/api/account", badPaymentAccount2))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid payment account id"));

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
