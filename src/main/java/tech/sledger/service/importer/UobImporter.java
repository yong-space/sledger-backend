package tech.sledger.service.importer;

import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class UobImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public List<Transaction> process(
        Account account, InputStream inputStream, List<Template> templates
    ) {
        Map<Integer, List<Object>> data = parseExcel(inputStream);

        String accountNumber = data.get(4).get(1).toString();
        long accountNumberLength = accountNumber.length();

        if (
            (account.getType() == Cash && accountNumberLength != 10) ||
            (account.getType() == Credit && accountNumberLength != 16)
        ) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }

        return account.getType() == AccountType.Cash ?
            processCash(data, account, templates) : processCredit(data, account, templates);
    }

    private Map<Integer, List<Object>> parseExcel(InputStream inputStream) {
        Map<Integer, List<Object>> data = new HashMap<>();

        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
            HSSFSheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<Object> dataRow = new ArrayList<>();
                for (Cell cell : row) {
                    var value = cell.getCellType().equals(NUMERIC) ?
                        cell.getNumericCellValue() : cell.getStringCellValue();
                    dataRow.add(value);
                }
                data.put(row.getRowNum(), dataRow);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }
        return data;
    }

    private List<Transaction> processCash(Map<Integer, List<Object>> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();

        for (int i = 8; i < data.size(); i++) {
            List<Object> dataRow = data.get(i);
            String dateContents = dataRow.getFirst().toString();
            if (!hasText(dateContents)) {
                continue;
            }
            Instant date = LocalDate.parse(dateContents, dateFormat)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
            String remarks = dataRow.get(1).toString().trim();
            BigDecimal debit = parseDecimal(dataRow.get(2).toString());
            BigDecimal credit = parseDecimal(dataRow.get(3).toString());
            Template template = matchTemplate(remarks, templates);

            output.add(CashTransaction.builder()
                .date(date)
                .remarks(template.getRemarks())
                .category(template.getCategory())
                .subCategory(template.getSubCategory())
                .amount(credit.subtract(debit))
                .accountId(account.getId())
                .build());
        }
        return output;
    }

    private List<Transaction> processCredit(
        Map<Integer, List<Object>> data, Account account, List<Template> templates
    ) {
        List<Transaction> output = new ArrayList<>();

        for (int i = 11; i <= data.size(); i++) {
            List<Object> dataRow = data.get(i);
            LocalDate localDate = LocalDate.parse(dataRow.getFirst().toString(), dateFormat);
            Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant billingMonth = getBillingMonth(localDate, (CreditAccount) account);
            String remarks = dataRow.get(2).toString().trim();
            BigDecimal amount = parseDecimal(dataRow.get(6).toString());
            Template template = matchTemplate(remarks, templates);

            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            output.add(CreditTransaction.builder()
                .date(date)
                .billingMonth(billingMonth)
                .remarks(template.getRemarks())
                .category(template.getCategory())
                .subCategory(template.getSubCategory())
                .amount(amount.multiply(BigDecimal.valueOf(-1)))
                .accountId(account.getId())
                .build());
        }
        return output;
    }
}
