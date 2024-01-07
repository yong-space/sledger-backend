package tech.sledger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashTests extends BaseTest {
    private static long creditAccountId;
    private static ZonedDateTime epoch;

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

        epoch = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .minusMonths(12);

        List<Map<String, ?>> transactions = List.of(
            tx(epoch.plusMonths(2).plusDays(1), "Insight A", 8),
            tx(epoch.plusMonths(2).plusDays(2), "Insight A", -2),
            tx(epoch.plusMonths(2).plusDays(3), "Insight B", -23),
            tx(epoch.plusMonths(2).plusDays(4), "Insight B", 15),
            tx(epoch.plusMonths(3).plusDays(5), "Insight A", 20),
            tx(epoch.plusMonths(3).plusDays(6), "Insight A", -14),
            tx(epoch.plusMonths(3).plusDays(7), "Insight B", -67),
            tx(epoch.plusMonths(3).plusDays(8), "Insight B", 51)
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
    public void creditCardStatements() throws Exception {
        Instant month = epoch.plusMonths(2).toInstant();
        mvc.perform(get("/api/dash/credit-card-statement/" + creditAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[?(@.month == '" + month + "')].amount").value(-2.0))
            .andExpect(jsonPath("$.[?(@.month == '" + month + "')].transactions").value(4));
    }

    Map<String, ?> tx(ZonedDateTime date, String category, int amount) {
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
