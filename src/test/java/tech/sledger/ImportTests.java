package tech.sledger;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tech.sledger.model.account.*;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import java.io.ByteArrayInputStream;
import java.util.List;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ImportTests extends BaseTest {
    private long ocbcCashAccountId;
    private long ocbcCreditAccountId;
    private long ocbcCreditAccountId2;
    private long cpfAccountId;
    byte[] ocbcCashCsv = """
Account details for:,360 Account 111-111111-001
Available Balance,"1,234.56"
Ledger Balance,"1,234.56"

Transaction History
Transaction date,Value date,Description,Withdrawals (SGD),Deposits (SGD)
16/05/2023,16/05/2023,COLLECTION,110.39,
,,SHOPPING ITEM
13/05/2023,13/05/2023,FAST PAYMENT,379.50,
,,OTHR - Payment via PayNow-UEN
            """.getBytes(UTF_8);
    byte[] ocbcCreditCsv = """
Account details for:,OCBC 365 Credit Card 1111-1111-1111-1111
Credit limit,"SGD 1,234.56"
Credit left,"SGD 321.23"

Transaction History
Main Credit CardOCBC 365 Credit Card 1111-1111-1111-1111
Transaction date,Description,Withdrawals (SGD),Deposits (SGD)
04/05/2023,SHOPPING,30.41,
01/05/2023,Household Stuff,0.12,
            """.getBytes(UTF_8);

    @PostConstruct
    public void init() {
        AccountIssuer accountIssuerA = new AccountIssuer();
        accountIssuerA.setName("OCBC");
        accountIssuerA = accountIssuerService.add(accountIssuerA);

        AccountIssuer accountIssuerB = new AccountIssuer();
        accountIssuerB.setName("CPF");
        accountIssuerB = accountIssuerService.add(accountIssuerB);

        User user = userService.get("basic-user@company.com");

        ocbcCashAccountId = accountService.add(CashAccount.builder()
            .issuer(accountIssuerA)
            .name("My Cash Account")
            .owner(user)
            .type(AccountType.Cash)
            .build()).getId();
        ocbcCreditAccountId = accountService.add(CreditAccount.builder()
            .issuer(accountIssuerA)
            .name("My Credit Account")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(1)
            .build()).getId();
        ocbcCreditAccountId2 = accountService.add(CreditAccount.builder()
            .issuer(accountIssuerA)
            .name("My Credit Account 2")
            .owner(user)
            .type(AccountType.Credit)
            .billingCycle(25)
            .build()).getId();
        cpfAccountId = accountService.add(CPFAccount.builder()
            .issuer(accountIssuerB)
            .name("CPF")
            .owner(user)
            .type(AccountType.Retirement)
            .build()).getId();

        Template template = Template.builder()
            .id(1).reference("shop").remarks("Stuff").category("Shopping").build();
        templateService.add(user, List.of(template));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void unsupportedIssuer() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "a.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ByteArrayInputStream(ocbcCashCsv)
        );

        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(cpfAccountId).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unsupported issuer"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void wrongFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "a.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ByteArrayInputStream(ocbcCashCsv)
        );

        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));

        file = new MockMultipartFile(
            "file",
            "a.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ByteArrayInputStream(ocbcCreditCsv)
        );

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCashAccountId).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid import file"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void ocbcCash() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "a.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ByteArrayInputStream(ocbcCashCsv)
        );

        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCashAccountId).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(2)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Stuff')]").exists())
            .andExpect(jsonPath("$.[?(@.category == 'Shopping')]").exists());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void ocbcCredit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "a.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            new ByteArrayInputStream(ocbcCreditCsv)
        );

        var request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", iterableWithSize(2)))
            .andExpect(jsonPath("$.[?(@.remarks == 'Household Stuff')]").exists());

        request = MockMvcRequestBuilders
            .multipart("/api/import")
            .part(new MockPart("accountId", String.valueOf(ocbcCreditAccountId2).getBytes()))
            .file(file);
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].billingMonth").value("2023-04-01T00:00:00Z"));
    }
}
