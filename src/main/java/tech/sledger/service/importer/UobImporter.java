package tech.sledger.service.importer;

import jxl.Sheet;
import jxl.Workbook;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;

public class UobImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public List<Transaction> process(
        Account account, InputStream inputStream, List<Template> templates
    ) {
        try {
            Sheet sheet = Workbook.getWorkbook(inputStream).getSheet(0);
            long accountNumberLength = sheet.getCell(1, 4).getContents().length();
            if (
                (account.getType() == Cash && accountNumberLength != 10) ||
                (account.getType() == Credit && accountNumberLength != 16)
            ) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
            }
            return account.getType() == AccountType.Cash ?
                processCash(sheet, account, templates) : processCredit(sheet, account, templates);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }
    }

    private List<Transaction> processCash(Sheet sheet, Account account, List<Template> templates) {
        int index = 1;
        List<Transaction> output = new ArrayList<>();

        for (int i = 8; i < sheet.getRows(); i++) {
            String dateContents = sheet.getCell(0, i).getContents();
            if (!hasText(dateContents)) {
                continue;
            }
            Instant date = LocalDate.parse(dateContents, dateFormat)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
            String remarks = sheet.getCell(1, i).getContents().trim();
            BigDecimal debit = parseDecimal(sheet.getCell(2, i).getContents());
            BigDecimal credit = parseDecimal(sheet.getCell(3, i).getContents());
            String category = "";

            Template template = matchTemplate(remarks, templates);
            if (template != null) {
                remarks = template.getRemarks();
                category = template.getCategory();
            }

            output.add(CashTransaction.builder()
                .id(index++)
                .date(date)
                .remarks(remarks)
                .category(category)
                .amount(credit.subtract(debit))
                .accountId(account.getId())
                .build());
        }
        return output;
    }

    private List<Transaction> processCredit(
        Sheet sheet, Account account, List<Template> templates
    ) {
        int index = 1;
        List<Transaction> output = new ArrayList<>();

        for (int i = 11; i < sheet.getRows(); i++) {
            LocalDate localDate = LocalDate.parse(sheet.getCell(0, i).getContents(), dateFormat);
            Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant billingMonth = getBillingMonth(localDate, (CreditAccount) account);
            String remarks = sheet.getCell(2, i).getContents().trim();
            BigDecimal amount = parseDecimal(sheet.getCell(6, i).getContents());
            String category = "";

            Template template = matchTemplate(remarks, templates);
            if (template != null) {
                remarks = template.getRemarks();
                category = template.getCategory();
            }

            output.add(CreditTransaction.builder()
                .id(index++)
                .date(date)
                .billingMonth(billingMonth)
                .remarks(remarks)
                .category(category)
                .amount(amount)
                .accountId(account.getId())
                .build());
        }
        return output;
    }
}
