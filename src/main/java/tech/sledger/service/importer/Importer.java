package tech.sledger.service.importer;

import static org.springframework.util.StringUtils.hasText;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.CreditAccount;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public interface Importer {
    default BigDecimal parseDecimal(String input) {
        String cleanInput = input.replaceAll(",", "");
        return hasText(cleanInput) ?
            new BigDecimal(cleanInput) : BigDecimal.ZERO;
    }

    default Template matchTemplate(String input, List<Template> templates) {
        String lowerInput = input.toLowerCase();
        return templates.stream()
            .filter(t ->lowerInput.contains(t.getReference()))
            .findFirst().orElse(null);
    }

    default Instant getBillingMonth(LocalDate date, CreditAccount account) {
        int cycle = account.getBillingCycle();
        LocalDate localMonth = date.withDayOfMonth(1);
        if (date.getDayOfMonth() < cycle) {
            localMonth = localMonth.minusMonths(1).withDayOfMonth(1);
        }
        return localMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    List<Transaction> process(
        Account account,
        InputStream inputStream,
        List<Template> templates
    ) throws Exception;
}
