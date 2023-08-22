package tech.sledger.endpoints;

import static java.util.stream.Collectors.groupingBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.CreditCardStatement;
import tech.sledger.model.dto.Insight;
import tech.sledger.model.dto.InsightChartSeries;
import tech.sledger.model.dto.InsightsResponse;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.TransactionRepo;
import tech.sledger.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dash")
public class DashEndpoints {
    private final UserService userService;
    private final AccountRepo accountRepo;
    private final TransactionRepo txRepo;

    @GetMapping("insights")
    public InsightsResponse getInsights(Authentication auth) {
        User user = (User) auth.getPrincipal();
        ZonedDateTime reference = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .withDayOfMonth(1);
        Instant to = reference.minusSeconds(1).toInstant();
        Instant from = reference.minusMonths(12).toInstant();
        List<Insight> raw = accountRepo.getInsights(user.getId(), from, to);

        Map<String, String> stackMap = new HashMap<>();
        List<CategoryInsight> summary = new ArrayList<>();

        raw.stream()
            .collect(groupingBy(Insight::getCategory, groupingBy(Insight::getSubCategory)))
            .forEach((category, value) -> value.forEach((subCategory, txList) -> {
                int transactions = txList.stream()
                    .map(Insight::getTransactions)
                    .reduce(0, Integer::sum);
                BigDecimal average = txList.stream()
                    .map(Insight::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(12L), 2, RoundingMode.HALF_EVEN);
                stackMap.put(category, average.compareTo(BigDecimal.ZERO) < 0 ? "debit" : "credit");
                summary.add(CategoryInsight.builder()
                    .category(category)
                    .subCategory(subCategory)
                    .transactions(transactions)
                    .average(average)
                    .build());
            }));

        List<Instant> months = new ArrayList<>();
        ZonedDateTime month = ZonedDateTime.ofInstant(from, ZoneOffset.UTC);
        ZonedDateTime toMonth = ZonedDateTime.ofInstant(to, ZoneOffset.UTC);
        while (month.isBefore(toMonth)) {
            months.add(month.toInstant());
            month = month.plusMonths(1);
        }

        List<InsightChartSeries> series = raw.parallelStream()
            .collect(groupingBy(Insight::getCategory))
            .entrySet().parallelStream().map(entry -> {
                List<BigDecimal> data = new ArrayList<>();
                for (Instant m : months) {
                    data.add(
                        entry.getValue().stream()
                            .filter(e -> e.getMonth().equals(m))
                            .findFirst()
                            .orElse(Insight.builder().total(BigDecimal.ZERO).build())
                            .getTotal().abs()
                    );
                }
                return InsightChartSeries.builder()
                    .label(entry.getKey())
                    .id(entry.getKey())
                    .data(data)
                    .stack(stackMap.get(entry.getKey()))
                    .build();
            })
            .toList();

        return InsightsResponse.builder()
            .xAxis(months)
            .series(series)
            .summary(summary)
            .build();
    }

    @GetMapping("credit-card-statement/{accountId}")
    public List<CreditCardStatement> getCreditCardStatement(
        Authentication auth, @PathVariable long accountId
    ) {
        userService.authorise(auth, accountId);
        return txRepo.getCreditCardStatement(accountId);
    }
}
