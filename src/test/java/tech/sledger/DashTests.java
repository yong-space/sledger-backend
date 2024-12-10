package tech.sledger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.DashTests.SubmitMethod.POST;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import tech.sledger.repo.UserRepo;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashTests {
    @Autowired
    public MockMvc mvc;
    @Autowired
    public TransactionRepo txRepo;
    @Autowired
    public UserService userService;
    @Autowired
    public AccountService accountService;
    @Autowired
    public AccountIssuerService accountIssuerService;
    @Autowired
    public AccountRepo accountRepo;
    @Autowired
    public UserRepo userRepo;
    @Autowired
    public PasswordEncoder passwordEncoder;
    @Autowired
    public ObjectMapper objectMapper;

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    static {
        System.setProperty("SLEDGER_SECRET_KEY", "my-secret-key");
        mongodb.start();
    }

    private static long creditAccountId;
    private static final ZonedDateTime epoch = LocalDate.now()
        .atStartOfDay(ZoneOffset.UTC)
        .withDayOfMonth(1)
        .minusMonths(12);

    @BeforeAll
    public void init() {
        txRepo.deleteAll();
        userRepo.save(User.builder()
            .id(1)
            .displayName("Basic User 1")
            .username("basic-user@company.com")
            .password(passwordEncoder.encode("B4SicUs3r!"))
            .enabled(true)
            .build());
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
            .andExpect(jsonPath("$.series[?(@.id == '+Insight A')].data[2]").value(6))
            .andExpect(jsonPath("$.series[?(@.id == '-Insight B')].stack").value("Debit"))
            .andExpect(jsonPath("$.summary[?(@.category == 'Insight A')].average").value(1))
            .andExpect(jsonPath("$.summary[?(@.category == 'Insight B')].average").value(-2));
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void creditCardBills() throws Exception {
        Instant month = epoch.plusMonths(2).toInstant();
        mvc.perform(get("/api/dash/credit-card-bills/" + creditAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[?(@.month == '" + month + "')].amount").value(-2))
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
        for (int i=0; i < 11; i++) {
            transactions1.add(cashTx(cashAccountId1, epoch.plusMonths(i).plusDays(i), "Month " + i, i));
            transactions2.add(cashTx(cashAccountId2, epoch.plusMonths(i).plusDays(i), "Month " + i, i + 1));
            transactions3.add(creditTx(epoch.plusMonths(i).plusDays(i), "Month " + i, i));
        }
        transactions1.add(cashTx(cashAccountId1, epoch.plusMonths(12), "Month 12", 12));
        transactions2.add(cashTx(cashAccountId2, epoch.plusMonths(12), "Month 12", 12));
        transactions3.add(creditTx(epoch.plusMonths(12), "Month 12", 12));
        mvc.perform(request(POST, "/api/transaction", transactions1)).andExpect(status().isOk());
        mvc.perform(request(POST, "/api/transaction", transactions2)).andExpect(status().isOk());
        mvc.perform(request(POST, "/api/transaction", transactions3)).andExpect(status().isOk());

        mvc.perform(get("/api/dash/balance-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series", hasSize(3)))
            .andExpect(jsonPath("$.series[?(@.id == " + cashAccountId1 + ")].data[11]").value(134))
            .andExpect(jsonPath("$.series[?(@.id == " + cashAccountId2 + ")].data[11]").value(78))
            .andExpect(jsonPath("$.series[?(@.id == 'total')].data[11]").value(212));
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

    public enum SubmitMethod { PUT, POST }
    public MockHttpServletRequestBuilder request(SubmitMethod method, String uri, Object payload) {
        try {
            MockHttpServletRequestBuilder builder = switch (method) {
                case PUT -> put(uri);
                case POST -> post(uri);
            };
            return builder.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
