package tech.sledger.service.importer;

import static java.nio.charset.StandardCharsets.UTF_8;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.CashTransaction;
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

@Slf4j
public class GrabImporter implements Importer {
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mma");
    private final List<String> rideServices = List.of("JustGrab", "GrabPet", "4 Seats GrabCar");

    @Override
    public List<Transaction> process(
        Account account, InputStream inputStream, List<Template> templates
    ) throws Exception {
        String rawContent = new String(inputStream.readAllBytes(), UTF_8).trim();
        StringReader stringReader = new StringReader(rawContent);
        List<String[]> data;
        try (CSVReader reader = new CSVReaderBuilder(stringReader).withSkipLines(1).build()) {
            data = reader.readAll();
        }
        List<Transaction> output = new ArrayList<>();

        for (String[] row : data) {
            Instant date = LocalDate.parse(row[0], dateFormat)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
            String service = row[4];
            boolean isRide = rideServices.contains(service);
            boolean isFood = row[4].equals("GrabFood");
            String category = isRide ? "Transport" : isFood ? "Food" : "Groceries";
            String subCategory = isRide ? "Private" : isFood ? "Food" : "Groceries";

            String from = matchTemplate(row[2], templates).getRemarks();
            String to = matchTemplate(row[3], templates).getRemarks();
            String remarks = isRide ? "Grab: " + from + " to " + to
                : service + ": " + from;

            CashTransaction tx = CashTransaction.builder()
                .date(date)
                .remarks(remarks)
                .category(category)
                .subCategory(subCategory)
                .amount(BigDecimal.ZERO.subtract(parseDecimal(row[6])))
                .accountId(account.getId())
                .build();
            output.add(tx);
        }
        return output;
    }
}
