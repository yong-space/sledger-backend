package tech.sledger.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;
import static tech.sledger.model.account.AccountType.Cash;
import static tech.sledger.model.account.AccountType.Credit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {
    private final TemplateService templateService;
    private final DateTimeFormatter ocbcDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<Transaction> process(
        Account account,
        InputStream inputStream
    ) throws IOException, CsvException {
        List<Template> templates = templateService.list(account.getOwner());
        return switch (account.getIssuer().getName()) {
            case "OCBC" -> processOcbc(account, inputStream, templates);
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported issuer");
        };
    }

    private BigDecimal parseDecimal(String input) {
        String cleanInput = input.replaceAll(",", "");
        return hasText(cleanInput) ? new BigDecimal(cleanInput) : BigDecimal.ZERO;
    }

    private Template matchTemplate(String input, List<Template> templates) {
        String lowerInput = input.toLowerCase();
        return templates.stream().filter(t -> lowerInput.contains(t.getReference())).findFirst().orElse(null);
    }

    private List<Transaction> processOcbc(
        Account account, InputStream inputStream, List<Template> templates
    ) throws IOException, CsvException {
        String rawContent = new String(inputStream.readAllBytes(), UTF_8).trim();
        String secondLine = rawContent.split("\n")[1];
        if (
            (account.getType() == Cash && !secondLine.startsWith("Available Balance")) ||
            (account.getType() == Credit && !secondLine.startsWith("Credit limit"))
        ) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid import file");
        }

        StringReader stringReader = new StringReader(rawContent);
        int skipLines = account.getType() == Cash ? 6 : 7;
        try (CSVReader reader = new CSVReaderBuilder(stringReader).withSkipLines(skipLines).build()) {
            if (account.getType() == Cash) {
                return processOcbcCash(reader.readAll(), templates);
            } else {
                return processOcbcCredit(reader.readAll(), account, templates);
            }
        }
    }

    private List<Transaction> processOcbcCash(List<String[]> data, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        CashTransaction tx = new CashTransaction();
        int index = 1;
        for (String[] row : data) {
            if (hasText(row[0])) {
                Instant date = LocalDate.parse(row[0], ocbcDateFormat)
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

    private List<Transaction> processOcbcCredit(List<String[]> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        int index = 1;
        for (String[] row : data) {
            LocalDate localDate = LocalDate.parse(row[0], ocbcDateFormat);
            Instant date = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();

            int cycle = ((CreditAccount) account).getBillingCycle();
            LocalDate localMonth = localDate.with(ChronoField.DAY_OF_MONTH, 1);
            if (localDate.getDayOfMonth() < cycle) {
                localMonth = localMonth.minus(1, ChronoUnit.MONTHS)
                    .with(ChronoField.DAY_OF_MONTH, 1);
            }
            Instant billingMonth = localMonth.atStartOfDay(ZoneOffset.UTC).toInstant();

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
