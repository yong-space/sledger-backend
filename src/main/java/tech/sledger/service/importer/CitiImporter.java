package tech.sledger.service.importer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static tech.sledger.model.account.AccountType.Credit;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.InputStream;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class CitiImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("d/M/yyyy");

    @Override
    public List<Transaction> process(
        Account account, InputStream inputStream, List<Template> templates
    ) {
        try {
            if (account.getType() != Credit) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
            }
            String rawContent = new String(inputStream.readAllBytes(), UTF_8).trim().replaceFirst("^\uFEFF", "");
            CSVReader reader = new CSVReaderBuilder(new StringReader(rawContent)).build();
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty() || rows.get(0).length < 5) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
            }
            return rows.stream()
                .filter(row -> row.length >= 4)
                .map(row -> {
                    LocalDate localDate = LocalDate.parse(row[0], dateFormat);
                    Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                    Template template = matchTemplate(cleanRemarks(row[1]), templates);
                    Instant billingMonth = getBillingMonth(localDate, (CreditAccount) account, template.getCategory());
                    return (Transaction) CreditTransaction.builder()
                        .date(date)
                        .billingMonth(billingMonth)
                        .remarks(template.getRemarks())
                        .category(template.getCategory())
                        .subCategory(template.getSubCategory())
                        .amount(parseDecimal(row[2]).add(parseDecimal(row[3])))
                        .accountId(account.getId())
                        .build();
                })
                .toList();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error importing Citi file", e);
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }
    }

    private String cleanRemarks(String raw) {
        return raw
            .replaceAll("\\s{2,}", " ")
            .replaceFirst("(?i)(\\s?singapore)*[0-9\\s]*sgp?$", "")
            .trim();
    }
}
