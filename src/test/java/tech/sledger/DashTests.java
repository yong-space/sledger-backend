package tech.sledger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashTests extends BaseTest {
    private static long creditAccountId;
    private static ZonedDateTime epoch = LocalDate.now()
        .atStartOfDay(ZoneOffset.UTC)
        .withDayOfMonth(1)
        .minusMonths(12);

    @BeforeAll
    public void init() {
        txRepo.deleteAll();
    }

    @Test
    @Order(1)
    @WithUserDetails("basic-user@company.com")
    public void setup() throws Exception {
        AccountIssuer accountIssuerA = new AccountIssuer();
        accountIssuerA.setName("a");
        accountIssuerA = accountIssuerService.add(accountIssuerA);

        User user = userService.get("basic-user@company.com");

        creditAccountId = accountService.add(CreditAccount.builder()
            .issuer(accountIssuerA)
            .name("My Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .multiCurrency(false)
            .paymentRemarks("Credit Card Bill")
            .build()).getId();

        List<Map<String, ?>> transactions = List.of(
            creditTx(epoch.plusMonths(2).plusDays(1), "Insight A", 8),
            creditTx(epoch.plusMonths(2).plusDays(2), "Insight A", -2),
            creditTx(epoch.plusMonths(2).plusDays(3), "Insight B", -23),
            creditTx(epoch.plusMonths(2).plusDays(4), "Insight B", 15),
            creditTx(epoch.plusMonths(3).plusDays(5), "Insight A", 20),
            creditTx(epoch.plusMonths(3).plusDays(6), "Insight A", -14),
            creditTx(epoch.plusMonths(3).plusDays(7), "Insight B", -67),
            creditTx(epoch.plusMonths(3).plusDays(8), "Insight B", 51)
        );
        mvc.perform(request(POST, "/api/transaction", transactions))
            .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @WithUserDetails("basic-user@company.com")
    public void insights() throws Exception {
        mvc.perform(get("/api/dash/insights"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series[?(@.id == '+Insight A')].data[2]").value(6.0))
            .andExpect(jsonPath("$.series[?(@.id == '-Insight B')].stack").value("Debit"))
            .andExpect(jsonPath("$.summary[?(@.category == 'Insight A')].average").value(1.0))
            .andExpect(jsonPath("$.summary[?(@.category == 'Insight B')].average").value(-2.0));
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void creditCardBills() throws Exception {
        Instant month = epoch.plusMonths(2).toInstant();
        mvc.perform(get("/api/dash/credit-card-bills/" + creditAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[?(@.month == '" + month + "')].amount").value(-2.0))
            .andExpect(jsonPath("$.[?(@.month == '" + month + "')].transactions").value(4));
    }

    @Test
    @Order(4)
    @WithUserDetails("basic-user@company.com")
    public void balanceHistory() throws Exception {
        AccountIssuer accountIssuerB = new AccountIssuer();
        accountIssuerB.setName("b");
        accountIssuerB = accountIssuerService.add(accountIssuerB);

        User user = userService.get("basic-user@company.com");

        txRepo.deleteAll();
        accountRepo.deleteAll();

        long cashAccountId1 = accountService.add(CashAccount.builder()
            .issuer(accountIssuerB)
            .name("Cash Account 1")
            .owner(user)
            .type(AccountType.Cash)
            .multiCurrency(false)
            .build()).getId();
        long cashAccountId2 = accountService.add(CashAccount.builder()
            .issuer(accountIssuerB)
            .name("Cash Account 2")
            .owner(user)
            .type(AccountType.Cash)
            .multiCurrency(false)
            .build()).getId();

        List<Map<String, ?>> transactions1 = new ArrayList<>();
        List<Map<String, ?>> transactions2 = new ArrayList<>();
        List<Map<String, ?>> transactions3 = new ArrayList<>();
        for (int i=0; i < 13; i++) {
            transactions1.add(cashTx(cashAccountId1, epoch.plusMonths(i).plusDays(i), "Month " + i, i));
            transactions2.add(cashTx(cashAccountId2, epoch.plusMonths(i).plusDays(i), "Month " + i, i + 1));
            transactions3.add(creditTx(epoch.plusMonths(i).plusDays(i), "Month " + i, i));
        }
        mvc.perform(request(POST, "/api/transaction", transactions1)).andExpect(status().isOk());
        mvc.perform(request(POST, "/api/transaction", transactions2)).andExpect(status().isOk());
        mvc.perform(request(POST, "/api/transaction", transactions3)).andExpect(status().isOk());

        mvc.perform(get("/api/dash/balance-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series", hasSize(2)))
            .andExpect(jsonPath("$.series[?(@.id == " + cashAccountId1 + ")].data[12]").value(156))
            .andExpect(jsonPath("$.series[?(@.id == " + cashAccountId2 + ")].data[12]").value(91));
    }

    Map<String, ?> cashTx(long accountId, ZonedDateTime date, String category, int amount) {
        return Map.of(
            "@type", "cash",
            "date", date,
            "billingMonth", date.withDayOfMonth(1),
            "category", category,
            "accountId", accountId,
            "amount", amount,
            "balance", amount,
            "remarks", "x"
        );
    }

    Map<String, ?> creditTx(ZonedDateTime date, String category, int amount) {
        return Map.of(
            "@type", "credit",
            "date", date,
            "billingMonth", date.withDayOfMonth(1),
            "category", category,
            "subCategory", category,
            "accountId", creditAccountId,
            "amount", amount,
            "remarks", "x"
        );
    }
}
