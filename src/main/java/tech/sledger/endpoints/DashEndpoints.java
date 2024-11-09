package tech.sledger.endpoints;

import static java.util.stream.Collectors.groupingBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountType;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.ChartResponse;
import tech.sledger.model.dto.ChartSeries;
import tech.sledger.model.dto.CreditCardStatement;
import tech.sledger.model.dto.Insight;
import tech.sledger.model.dto.MonthlyBalance;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dash")
public class DashEndpoints {
    private final UserService userService;
    private final AccountRepo accountRepo;
    private final TransactionRepo txRepo;

    @GetMapping("insights")
    public ChartResponse getInsights(Authentication auth) {
        User user = (User) auth.getPrincipal();
        ZonedDateTime reference = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .withDayOfMonth(1);
        Instant to = reference.minusSeconds(1).toInstant();
        Instant from = reference.minusMonths(12).toInstant();
        List<Insight> raw = accountRepo.getInsights(user.getId(), from, to);

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

        List<ChartSeries> series = new ArrayList<>();

        raw.parallelStream()
            .collect(groupingBy(Insight::getCategory))
            .forEach((key, value) -> {
                List<BigDecimal> positives = new ArrayList<>();
                List<BigDecimal> negatives = new ArrayList<>();

                for (Instant m : months) {
                    BigDecimal total = value.stream()
                        .filter(e -> e.getMonth().equals(m))
                        .findFirst()
                        .orElse(Insight.builder().total(BigDecimal.ZERO).build())
                        .getTotal();

                    boolean isCredit = total.compareTo(BigDecimal.ZERO) > 0;
                    positives.add(isCredit ? total.abs() : BigDecimal.ZERO);
                    negatives.add(!isCredit ? total.abs() : BigDecimal.ZERO);
                }

                addChartSeries(series, positives, key, "Credit");
                addChartSeries(series, negatives, key, "Debit");
            });

        return ChartResponse.builder()
            .xAxis(months)
            .series(series)
            .summary(summary)
            .build();
    }

    private void addChartSeries(List<ChartSeries> series, List<BigDecimal> data, String name, String stack) {
        BigDecimal sum = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (BigDecimal.ZERO.equals(sum)) {
            return;
        }
        String key = (stack.equals("Credit") ? "+" : "-") + name;
        series.add(ChartSeries.builder()
            .label(key)
            .id(key)
            .data(data)
            .stack(stack)
            .build());
    }

    @GetMapping("credit-card-bills/{accountId}")
    public List<CreditCardStatement> getCreditCardBills(
        Authentication auth, @PathVariable("accountId") long accountId
    ) {
        userService.authorise(auth, accountId);
        return txRepo.getCreditCardBills(accountId);
    }

    @GetMapping("balance-history")
    public ChartResponse getBalanceHistory(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<Long> accountIds = accountRepo.findAllByOwnerAndTypeIn(user, List.of(AccountType.Cash, AccountType.Credit))
            .stream().map(Account::getId).toList();
        ZonedDateTime reference = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .withDayOfMonth(1);
        Instant from = reference.minusMonths(12).toInstant();
        List<MonthlyBalance> balanceHistory = txRepo.getBalanceHistory(accountIds, from, Instant.now());

        List<Instant> months = balanceHistory.stream().map(MonthlyBalance::getMonth).distinct().sorted().toList();

        Map<Long, String> accountNames = accountRepo.findAllById(accountIds)
            .stream().collect(Collectors.toMap(Account::getId, Account::getName));

        List<BigDecimal> summaryData = new ArrayList<>();

        var accountSeries = accountIds.stream().map(accountId -> {
            AtomicInteger monthIndex = new AtomicInteger();
            List<BigDecimal> data = months.stream()
                .map(month -> {
                    Optional<MonthlyBalance> value = balanceHistory.stream()
                        .filter(b -> b.getAccountId() == accountId && b.getMonth().equals(month))
                        .findFirst();
                    return value.get();
                })
                .map(MonthlyBalance::getBalance)
                .peek(balance -> {
                    if (summaryData.size() < months.size()) {
                        summaryData.add(balance);
                    } else {
                        summaryData.set(monthIndex.get(), summaryData.get(monthIndex.get()).add(balance));
                        monthIndex.getAndIncrement();
                    }
                })
                .toList();
            return ChartSeries.builder()
                .id(accountId.toString())
                .label(accountNames.get(accountId))
                .stack("history")
                .data(data)
                .build();
        }).toList();

        List<ChartSeries> series = new ArrayList<>(accountSeries);
        series.add(ChartSeries.builder()
            .id("total")
            .label("Total")
            .data(summaryData)
            .type("line")
            .build());

        return ChartResponse.builder()
            .xAxis(months)
            .series(series)
            .build();
    }
}
