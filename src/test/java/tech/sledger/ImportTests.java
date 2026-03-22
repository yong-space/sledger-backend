package tech.sledger;

import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CPFAccount;
import tech.sledger.model.account.CashAccount;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import tech.sledger.repo.UserRepo;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.TemplateService;
import tech.sledger.service.UserService;
import java.io.IOException;
import java.util.List;

@SpringBootTest(classes = Sledger.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportTests {
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:8");

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    static {
        System.setProperty("SLEDGER_SECRET_KEY", "my-secret-key");
        mongodb.start();
    }

    @Autowired
    public MockMvc mvc;
    @Autowired
    public AccountIssuerService accountIssuerService;
    @Autowired
    public AccountService accountService;
    @Autowired
    public UserService userService;
    @Autowired
    public UserRepo userRepo;
    @Autowired
    public PasswordEncoder passwordEncoder;
    @Autowired
    public TemplateService templateService;

    private long ocbcCashAccountId;
    private long ocbcCreditAccountId;
    private long ocbcCreditAccountId2;
    private long uobCashAccountId;
    private long uobCreditAccountId;
    private long uobCreditAccountId2;
    private long grabAccountId;
    private long cpfAccountId;
    private long citiCashAccountId;
    private long citiCreditAccountId;
    private long citiCreditAccountId2;

    @PostConstruct
    public void init() {
        AccountIssuer ocbcIssuer = new AccountIssuer();
        ocbcIssuer.setName("OCBC");
        ocbcIssuer = accountIssuerService.add(ocbcIssuer);

        AccountIssuer uobIssuer = new AccountIssuer();
        uobIssuer.setName("UOB");
        uobIssuer = accountIssuerService.add(uobIssuer);

        AccountIssuer grabIssuer = new AccountIssuer();
        grabIssuer.setName("Grab");
        grabIssuer = accountIssuerService.add(grabIssuer);

        AccountIssuer cpfIssuer = new AccountIssuer();
        cpfIssuer.setName("CPF");
        cpfIssuer = accountIssuerService.add(cpfIssuer);

        AccountIssuer citiIssuer = new AccountIssuer();
        citiIssuer.setName("Citi");
        citiIssuer = accountIssuerService.add(citiIssuer);

        User user = userRepo.save(User.builder()
            .id(1)
            .displayName("Basic User 1")
            .username("basic-user@company.com")
            .password(passwordEncoder.encode("B4SicUs3r!"))
            .enabled(true)
            .build());

        ocbcCashAccountId = accountService.add(CashAccount.builder()
            .issuer(ocbcIssuer)
            .name("My Cash Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();
        ocbcCreditAccountId = accountService.add(CreditAccount.builder()
            .issuer(ocbcIssuer)
            .name("My Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .build()).getId();
        ocbcCreditAccountId2 = accountService.add(CreditAccount.builder()
            .issuer(ocbcIssuer)
            .name("My Credit Account 2")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(25)
            .build()).getId();
        uobCashAccountId = accountService.add(CashAccount.builder()
            .issuer(uobIssuer)
            .name("My Cash Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();
        uobCreditAccountId = accountService.add(CreditAccount.builder()
            .issuer(uobIssuer)
            .name("My Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .build()).getId();
        uobCreditAccountId2 = accountService.add(CreditAccount.builder()
            .issuer(uobIssuer)
            .name("My Credit Account 2")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(25)
            .build()).getId();
        grabAccountId = accountService.add(CashAccount.builder()
            .issuer(grabIssuer)
            .name("My Grab Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();
        cpfAccountId = accountService.add(CPFAccount.builder()
            .issuer(cpfIssuer)
            .name("CPF")
            .owner(user)
            .type(AccountType.Retirement)
            .build()).getId();
        citiCashAccountId = accountService.add(CashAccount.builder()
            .issuer(citiIssuer)
            .name("My Citi Cash Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();
        citiCreditAccountId = accountService.add(CreditAccount.builder()
            .issuer(citiIssuer)
            .name("My Citi Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .build()).getId();
        citiCreditAccountId2 = accountService.add(CreditAccount.builder()
            .issuer(citiIssuer)
            .name("My Citi Credit Account 2")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(25)
            .build()).getId();

        Template template1 = Template.builder()
            .id(1L).reference("shop").remarks("Stuff").category("Gifts").subCategory("Shopping").build();
        Template template2 = Template.builder()
            .id(2L).reference("credit card bill").remarks("Credit Card Bill").category("Credit Card Bill").subCategory("Credit Card Bill").build();
        templateService.add(user, List.of(template1, template2));
    }

    private MockMultipartFile mockFile(String filename) throws IOException {
        return new MockMultipartFile(
            "file",
            filename,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ClassPathResource(filename).getInputStream()
        );
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void unsupportedIssuer() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(cpfAccountId).getBytes()))
            .file(mockFile("ocbc-cash.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unsupported issuer"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void wrongFile() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId).getBytes()))
            .file(mockFile("uob-cash.xls"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId).getBytes()))
            .file(mockFile("ocbc-cash.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCashAccountId).getBytes()))
            .file(mockFile("ocbc-credit.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCashAccountId).getBytes()))
            .file(mockFile("ocbc-credit.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCreditAccountId).getBytes()))
            .file(mockFile("uob-cash.xls"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCashAccountId).getBytes()))
            .file(mockFile("uob-credit.xls"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCashAccountId).getBytes()))
            .file(mockFile("citi.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId).getBytes()))
            .file(mockFile("ocbc-cash.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId).getBytes()))
            .file(mockFile("citi-empty.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId).getBytes()))
            .file(mockFile("citi-bad-date.csv"));
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void ocbcCash() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCashAccountId).getBytes()))
            .file(mockFile("ocbc-cash.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(5)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Sample Case Test')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Gifts')]").exists())
            .andExpect(jsonPath("$.[?(@.subCategory == 'Shopping')]").exists())
            .andExpect(jsonPath("$.[?(@.remarks == 'Interest 1')]").exists())
            .andExpect(jsonPath("$.[?(@.remarks == 'Interest 2')]").exists());

        var request2 = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCashAccountId).getBytes()))
            .file(mockFile("ocbc-cash-2.csv"));
        mvc.perform(request2).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void ocbcCredit() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId).getBytes()))
            .file(mockFile("ocbc-credit.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(5)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Bus/Mrt Cm8880515')]").exists());

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId2).getBytes()))
            .file(mockFile("ocbc-credit.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].billingMonth").value("2024-08-01T00:00:00Z"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void uobCash() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCashAccountId).getBytes()))
            .file(mockFile("uob-cash.xls"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(2)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Stuff')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Gifts')]").exists())
            .andExpect(jsonPath("$.[?(@.subCategory == 'Shopping')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void uobCredit() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCreditAccountId).getBytes()))
            .file(mockFile("uob-credit.xls"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(2)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Household Stuff')]").exists());

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(uobCreditAccountId2).getBytes()))
            .file(mockFile("uob-credit.xls"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].amount").value("100.0"))
            .andExpect(jsonPath("$.[0].billingMonth").value("2023-04-01T00:00:00Z"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void grab() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(grabAccountId).getBytes()))
            .file(mockFile("grab.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(5)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Grab: Somewhere to Somewhere Else')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Transport')]").exists())
            .andExpect(jsonPath("$.[?(@.subCategory == 'Private')]").exists())
            .andExpect(jsonPath("$.[?(@.remarks == 'GrabFood: Somewhere')]").exists())
            .andExpect(jsonPath("$.[?(@.remarks == 'GrabMart: Somewhere')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Groceries')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Food')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void citiCredit() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId).getBytes()))
            .file(mockFile("citi.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(23)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Lazada')]").exists())
            .andExpect(jsonPath("$.[?(@.remarks == 'Kopitiam Fp App Paymen')]").exists());

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId2).getBytes()))
            .file(mockFile("citi.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].billingMonth").value("2026-01-01T00:00:00Z"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void citiCreditShortRows() throws Exception {
        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(citiCreditAccountId).getBytes()))
            .file(mockFile("citi-short-rows.csv"));
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(1)));
    }
}
