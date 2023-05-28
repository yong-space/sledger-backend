package tech.sledger.service.importer;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;

public class OcbcImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<Transaction> process(
        Account account, InputStream inputStream, List<Template> templates
    ) {
        try {
            String rawContent = new String(inputStream.readAllBytes(), UTF_8).trim();
            StringReader stringReader = new StringReader(rawContent);
            int skipLines = account.getType() == Cash ? 6 : 7;
            CSVReader reader = new CSVReaderBuilder(stringReader)
                .withSkipLines(skipLines).build();

            String secondLine = rawContent.split("\n")[1];
            if (
                (account.getType() == Cash && !secondLine.startsWith("Available Balance")) ||
                (account.getType() == Credit && !secondLine.startsWith("Credit limit"))
            ) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
            }

            if (account.getType() == Cash) {
                return processCash(reader.readAll(), templates);
            } else {
                return processCredit(reader.readAll(), account, templates);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }
    }

    private List<Transaction> processCash(List<String[]> data, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        CashTransaction tx = new CashTransaction();
        int index = 1;
        for (String[] row : data) {
            if (hasText(row[0])) {
                Instant date = LocalDate.parse(row[0], dateFormat)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
                BigDecimal debit = parseDecimal(row[3]);
                BigDecimal credit = parseDecimal(row[4]);
                tx = CashTransaction.builder()
                    .id(index++)
                    .date(date)
                    .remarks(row[2])
                    .amount(credit.subtract(debit))
                    .build();
            } else {
                tx.setRemarks(tx.getRemarks() + " " + row[2]);
                Template template = matchTemplate(tx.getRemarks(), templates);
                if (template != null) {
                    tx.setRemarks(template.getRemarks());
                    tx.setCategory(template.getCategory());
                }
                output.add(tx);
            }
        }
        return output;
    }

    private List<Transaction> processCredit(List<String[]> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        int index = 1;
        for (String[] row : data) {
            LocalDate localDate = LocalDate.parse(row[0], dateFormat);
            Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant billingMonth = getBillingMonth(localDate, (CreditAccount) account);

            String remarks = row[1];
            String category = null;
            Template template = matchTemplate(remarks, templates);
            if (template != null) {
                remarks = template.getRemarks();
                category = template.getCategory();
            }

            BigDecimal debit = parseDecimal(row[2]);
            BigDecimal credit = parseDecimal(row[3]);

            output.add(CreditTransaction.builder()
                .id(index++)
                .date(date)
                .billingMonth(billingMonth)
                .remarks(remarks)
                .category(category)
                .amount(credit.subtract(debit))
                .build());
        }
        return output;
    }
}
