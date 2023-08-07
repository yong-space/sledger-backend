package tech.sledger.endpoints;

import static java.util.Comparator.comparing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.Insight;
import tech.sledger.model.dto.InsightsResponse;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dash")
public class DashEndpoints {
    private final AccountRepo accountRepo;

    @GetMapping("insights")
    public InsightsResponse getInsights(Authentication auth) {
        User user = (User) auth.getPrincipal();
        ZonedDateTime reference = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .withDayOfMonth(1);
        Instant to = reference.minus(1, ChronoUnit.SECONDS).toInstant();
        Instant from = reference.minus(12, ChronoUnit.MONTHS).toInstant();
        List<Insight> data = accountRepo.getInsights(user.getId(), from, to);

        List<CategoryInsight> summary = data.parallelStream()
            .collect(Collectors.groupingBy(Insight::getCategory))
            .entrySet().parallelStream().map(entry ->
                CategoryInsight.builder()
                    .category(entry.getKey())
                    .transactions(entry.getValue().size())
                    .total(entry.getValue().stream().map(Insight::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .build()
            )
            .sorted(comparing(CategoryInsight::getTotal))
            .toList();

        return InsightsResponse.builder()
            .data(data).summary(summary).build();
    }
}
