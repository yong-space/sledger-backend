package tech.sledger.service.importer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class OcbcImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("d/M/yyyy");

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
                return processCash(reader.readAll(), account, templates);
            } else {
                return processCredit(reader.readAll(), account, templates);
            }
        } catch (Exception e) {
            log.error("Error importing OCBC file", e);
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }
    }

    private void completeTransaction(CashTransaction tx, String remarks, List<Template> templates) {
        Template template = matchTemplate(cleanCashRemarks(remarks), templates);
        tx.setRemarks(template.getRemarks());
        tx.setCategory(template.getCategory());
        tx.setSubCategory(template.getSubCategory());
    }

    private List<Transaction> processCash(List<String[]> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        CashTransaction tx = null;

        for (String[] row : data) {
            if (hasText(row[0])) {
                if (tx != null) {
                    completeTransaction(tx, tx.getRemarks(), templates);
                    output.add(tx);
                }
                Instant date = LocalDate.parse(row[0], dateFormat)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
                BigDecimal debit = parseDecimal(row[3]);
                BigDecimal credit = parseDecimal(row[4]);
                tx = CashTransaction.builder()
                    .date(date)
                    .remarks(row[2])
                    .amount(credit.subtract(debit))
                    .accountId(account.getId())
                    .build();
            } else {
                completeTransaction(Objects.requireNonNull(tx), tx.getRemarks() + " " + row[2], templates);
                output.add(tx);
                tx = null;
            }
        }
        if (tx != null) {
            completeTransaction(tx, tx.getRemarks(), templates);
            output.add(tx);
        }
        return output;
    }

    private List<Transaction> processCredit(List<String[]> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        for (String[] row : data) {
            LocalDate localDate = LocalDate.parse(row[0], dateFormat);
            Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Template template = matchTemplate(cleanCreditRemarks(row[1]), templates);
            Instant billingMonth = getBillingMonth(localDate, (CreditAccount) account, template.getCategory());
            BigDecimal debit = parseDecimal(row[2]);
            BigDecimal credit = parseDecimal(row[3]);

            output.add(CreditTransaction.builder()
                .date(date)
                .billingMonth(billingMonth)
                .remarks(template.getRemarks())
                .category(template.getCategory())
                .subCategory(template.getSubCategory())
                .amount(credit.subtract(debit))
                .accountId(account.getId())
                .build());
        }
        return output;
    }

    private String cleanCashRemarks(String raw) {
        return raw.replaceAll("\\s{2,}", " ")
            .replaceAll("[\\r\\n]+", "");
    }

    private String cleanCreditRemarks(String raw) {
        return cleanCashRemarks(raw)
            .replaceFirst("^-[0-9]+", "")
            .replaceFirst("(?i)(\\s?singapore)*[0-9\\s]*sgp?$", "");
    }
}
