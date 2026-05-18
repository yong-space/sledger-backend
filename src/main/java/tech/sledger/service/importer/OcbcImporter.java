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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern CREDIT_DATE_PAT = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}");

    private List<Transaction> processCredit(List<String[]> data, Account account, List<Template> templates) {
        List<Transaction> output = new ArrayList<>();
        for (String[] row : data) {
            if (row.length < 4 || !CREDIT_DATE_PAT.matcher(row[0]).matches()) {
                continue;
            }
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

    private static final Pattern NETS_QR_PAT = Pattern.compile(
        "(?i)Nets Qr ?(.+?) ?(?:\\d+ ?)?Nets Qr Purchase(?: ?\\d+)?");
    private static final Pattern NETS_QRIS_PAT = Pattern.compile(
        "(?i)Qris\\s+(.+)$");
    private static final Pattern FAST_OTHR_PAT = Pattern.compile(
        "(?i)^Fast\\s*(?:Payment|Transfer)\\s*(.*?)\\s+Othr\\s+(.*?)$");
    private static final Pattern FAST_PAYMENT_PAYNOW_PAT = Pattern.compile(
        "(?i)^Fast\\s*Payment\\s*othr-\\S*\\s+To\\s+(.*?)\\s+[A-Za-z]{0,3}via\\s+Paynow");
    private static final Pattern FUND_TRANSFER_PAYNOW_PAT = Pattern.compile(
        "(?i)^Fund\\s*Transfer\\s*othr\\s*-\\s*(.*?)\\s+To\\s+(.*?)\\s+[A-Za-z]{0,3}via\\s+Paynow");
    private static final Pattern FUND_TRANSFER_INTERNET_PAT = Pattern.compile(
        "(?i)^Fund\\s*Transfer\\s+\\S+\\s*-\\s*(.*?)\\s+from\\s+(.*?)\\s*Internet\\s+Banking$");
    private static final Pattern PAYMENT_TRANSFER_VIAFROM_PAT = Pattern.compile(
        "(?i)^Payment/Transfer\\s+Othr\\s+(.+?)\\s+via\\s+Paynow-\\S+\\s+from\\s+(.+)$");
    private static final Pattern PAYMENT_TRANSFER_FROMVIA_PAT = Pattern.compile(
        "(?i)^Payment/Transfer\\s+Othr\\s+(.+?)\\s+from\\s+(.*?)\\s*via\\s+Paynow-\\S+\\s*$");
    private static final Pattern PAYMENT_TRANSFER_FROM_PAT = Pattern.compile(
        "(?i)^Payment/Transfer\\s+Othr\\s+(.+?)\\s+from\\s+(.+?)$");

    private String cleanCashRemarks(String raw) {
        String cleaned = raw.replaceAll("\\s+", " ").trim()
            .replaceAll("(?i)(\\d)(Othr)\\b", "$1 $2");

        Matcher m = NETS_QR_PAT.matcher(cleaned);
        if (m.find()) {
            String detail = m.group(1).trim();
            return Character.toUpperCase(detail.charAt(0)) + detail.substring(1);
        }

        m = NETS_QRIS_PAT.matcher(cleaned);
        if (m.find()) {
            return capitalizeFirst(m.group(1).trim());
        }

        m = FAST_OTHR_PAT.matcher(cleaned);
        if (m.find()) {
            String merchant = cleanOthrMerchant(m.group(2).trim());
            String detail   = cleanOthrDetail(m.group(1).trim());
            if (!merchant.isEmpty()) {
                return detail.isEmpty()
                    ? capitalizeFirst(merchant)
                    : capitalizeFirst(merchant) + ": " + capitalizeFirst(detail);
            }
        }

        m = FAST_PAYMENT_PAYNOW_PAT.matcher(cleaned);
        if (m.find()) {
            return capitalizeFirst(m.group(1).trim());
        }

        m = FUND_TRANSFER_PAYNOW_PAT.matcher(cleaned);
        if (m.find()) {
            String detail    = stripFundTransferRefCode(m.group(1).trim());
            String recipient = m.group(2).trim()
                .replaceAll("(?i)\\s+Pte\\.?.*$", "")
                .trim();
            return detail.isEmpty()
                ? capitalizeFirst(recipient)
                : capitalizeFirst(recipient) + ": " + capitalizeFirst(detail);
        }

        m = FUND_TRANSFER_INTERNET_PAT.matcher(cleaned);
        if (m.find()) {
            String detail = m.group(1).trim();
            String sender = cleanFundTransferSender(m.group(2).trim());
            if (!sender.isEmpty()) {
                return detail.isEmpty()
                    ? capitalizeFirst(sender)
                    : capitalizeFirst(sender) + ": " + capitalizeFirst(detail);
            }
            return capitalizeFirst(detail);
        }

        m = PAYMENT_TRANSFER_VIAFROM_PAT.matcher(cleaned);
        if (m.find()) {
            return formatPaymentTransferPayNow(m.group(1), m.group(2));
        }
        m = PAYMENT_TRANSFER_FROMVIA_PAT.matcher(cleaned);
        if (m.find()) {
            return formatPaymentTransferPayNow(m.group(1), m.group(2));
        }
        m = PAYMENT_TRANSFER_FROM_PAT.matcher(cleaned);
        if (m.find()) {
            return formatPaymentTransferPayNow(m.group(1), m.group(2));
        }
        return cleaned;
    }

    private String formatPaymentTransferPayNow(String detail, String sender) {
        String cleanSender = sender.trim()
            .replaceAll("(?<=[a-z])[A-Z]{3,5}$", "")
            .trim();
        String cleanDetail = detail.trim();
        if (cleanDetail.chars().anyMatch(Character::isDigit)
            || cleanDetail.equalsIgnoreCase(cleanSender)) {
            cleanDetail = "";
        }
        String result = "PayNow from " + capitalizeFirst(cleanSender);
        if (!cleanDetail.isEmpty()) {
            result += ": " + capitalizeFirst(cleanDetail);
        }
        return result;
    }

    private String cleanOthrDetail(String s) {
        return s
            .replaceAll("(?i)epossp\\S+", "")
            .replaceAll("(?i)\\bsf\\d+\\b", "")
            .replaceAll("(?i)\\b[A-Za-z]{2,5}\\s+\\d{7,}\\b", "")
            .replaceAll("(?i)(?=\\S*\\d)(?=\\S*[A-Z])\\b[A-Z0-9]{12,}\\b", "")
            .replaceAll("\\b\\d{7,}\\b", "")
            .replaceAll("\\s{2,}", " ")
            .trim();
    }

    private String cleanOthrMerchant(String s) {
        return s
            .replaceAll("(?i)(?<=[\\s.])[A-Za-z]{2,5}\\s+\\d{7,}$", "")
            .replaceAll("\\s+\\d{8,}$", "")
            .replaceAll("[a-z]\\d{6,}$", "")
            .trim();
    }

    private String stripFundTransferRefCode(String s) {
        return s.chars().anyMatch(Character::isDigit) ? "" : s;
    }

    private String cleanFundTransferSender(String sender) {
        String trimmed = sender.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace > 0 && trimmed.indexOf(' ') != lastSpace
            && trimmed.substring(lastSpace + 1).matches("[A-Z]+")) {
            return trimmed.substring(0, lastSpace);
        }
        return trimmed;
    }

    private String capitalizeFirst(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String cleanCreditRemarks(String raw) {
        return cleanCashRemarks(raw)
            .replaceFirst("^-[0-9]+", "")
            .replaceFirst("(?i)(\\s?singapore)*[0-9\\s]*sgp?$", "");
    }
}
