package tech.sledger;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import tech.sledger.model.account.*;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import tech.sledger.repo.UserRepo;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.TemplateService;
import tech.sledger.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportTests {
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
    private long cpfAccountId;
    byte[] ocbcCashCsv = """
Account details for:,360 Account 111-111111-001
Available Balance,"1,234.56"
Ledger Balance,"1,234.56"

Transaction History
Transaction date,Value date,Description,Withdrawals (SGD),Deposits (SGD)
18/05/2023,16/05/2023,SAMPLE,12.34,
,,CASE TEST
17/05/2023,29/02/2024,INTEREST 1,,0.62
16/05/2023,16/05/2023,COLLECTION,110.39,
,,SHOPPING ITEM
13/05/2023,13/05/2023,FAST PAYMENT,379.50,
,,OTHR - Payment via PayNow-UEN
01/05/2023,29/02/2024,INTEREST 2,,0.62
            """.getBytes(UTF_8);
    byte[] ocbcCashCsv2 = """
Account details for:,360 Account 111-111111-001
Available Balance,"1,234.56"
Ledger Balance,"1,234.56"

Transaction History
Transaction date,Value date,Description,Withdrawals (SGD),Deposits (SGD)
13/05/2023,13/05/2023,FAST PAYMENT,379.50,
,,OTHR - Payment via PayNow-UEN
            """.getBytes(UTF_8);
    byte[] ocbcCreditCsv = """
Account details for:,OCBC 365 Credit Card 1111-1111-1111-1111
Credit limit,"SGD 1,234.56"
Credit left,"SGD 321.23"

Transaction history
Main credit card OCBC INFINITY Cashback Card 5413-8301-0004-9600
Transaction date,Description,Withdrawals (SGD),Deposits (SGD)
2024-09-02 00:00:00.0,-9489 KOUFU PTE LTD    Singapore     SGP,2.60,
2024-09-02 00:00:00.0,-9489 BUS/MRT 498357444SINGAPORE     SGP,7.62,
2024-08-31 00:00:00.0,-9489 BUS/MRT CM8880515SINGAPORE     SGP,,1.21
2024-08-31 00:00:00.0,-9489 PIZZAKAYA-JEM    SINGAPORE     SGP,46.28,
            """.getBytes(UTF_8);
    Map<String, byte[]> ocbcCsv = Map.of(
        "ocbc-cash.csv", ocbcCashCsv,
        "ocbc-cash-2.csv", ocbcCashCsv2,
        "ocbc-credit.csv", ocbcCreditCsv
    );

    @PostConstruct
    public void init() {
        AccountIssuer ocbcIssuer = new AccountIssuer();
        ocbcIssuer.setName("OCBC");
        ocbcIssuer = accountIssuerService.add(ocbcIssuer);

        AccountIssuer uobIssuer = new AccountIssuer();
        uobIssuer.setName("UOB");
        uobIssuer = accountIssuerService.add(uobIssuer);

        AccountIssuer cpfIssuer = new AccountIssuer();
        cpfIssuer.setName("CPF");
        cpfIssuer = accountIssuerService.add(cpfIssuer);

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

        cpfAccountId = accountService.add(CPFAccount.builder()
            .issuer(cpfIssuer)
            .name("CPF")
            .owner(user)
            .type(AccountType.Retirement)
            .build()).getId();

        Template template = Template.builder()
            .id(1).reference("shop").remarks("Stuff").category("Gifts").subCategory("Shopping").build();
        templateService.add(user, List.of(template));
    }

    private MockMultipartFile mockFile(String filename) throws IOException {
        InputStream inputStream = filename.endsWith(".csv") ?
            new ByteArrayInputStream(ocbcCsv.get(filename)) :
            new ClassPathResource(filename).getInputStream();
        return new MockMultipartFile(
            "file",
            filename,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            inputStream
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
            .andExpect(jsonPath("$", iterableWithSize(4)))
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
}
