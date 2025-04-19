package tech.sledger.endpoints;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.portfolio.EmailSnapshot;
import tech.sledger.model.portfolio.PortfolioPosition;
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.portfolio.PortfolioSummary;
import tech.sledger.model.user.User;
import tech.sledger.repo.PortfolioSnapshotRepo;
import tech.sledger.service.EmailService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioSnapshotEndpoints {
    @Value("${portfolio-endpoint:http://portfolio}")
    public String portfolioEndpoint;
    public final PortfolioSnapshotRepo portfolioSnapshotRepo;
    public final ObjectMapper mapper;
    public final HttpClient httpClient;
    public final EmailService emailService;

    @GetMapping
    public PortfolioSnapshot getSnapshot(Authentication auth) {
        User user = (User) auth.getPrincipal();
        PortfolioSnapshot snapshot = portfolioSnapshotRepo.findFirstByOwner(user);
        if (snapshot == null) {
            snapshot = refreshSnapshot(auth);
        }
        return snapshot;
    }

    @GetMapping("/refresh")
    public PortfolioSnapshot refreshSnapshot(Authentication auth) {
        User user = (User) auth.getPrincipal();
        PortfolioSnapshot snapshot = portfolioSnapshotRepo.findFirstByOwner(user);
        if (snapshot == null) {
            snapshot = PortfolioSnapshot.builder()
                .id(user.getId())
                .owner(user)
                .build();
        }
        PortfolioSummary portfolio = http("/summary", new TypeReference<>() {});
        snapshot.setCash(portfolio.getCash());
        snapshot.setHoldings(portfolio.getHoldings());
        snapshot.setFx(portfolio.getFx());
        snapshot.setTime(Instant.now());
        portfolioSnapshotRepo.save(snapshot);
        return snapshot;
    }

    @GetMapping(value = "/email-snapshot", produces = "text/html")
    public String emailSnapshot(Authentication auth) {
        User user = (User) auth.getPrincipal();
        EmailSnapshot snapshotData = buildSnapshotData();
        String content = emailService.compileTemplate("portfolio-snapshot", snapshotData);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        emailService.sendEmail(user.getUsername(), user.getDisplayName(), "Portfolio Snapshot for " + date, content);
        return content;
    }

    private EmailSnapshot buildSnapshotData() {
        List<PortfolioPosition> positions = http("/positions", new TypeReference<>() {});
        positions.sort((a, b) -> {
            if ("CASH".equals(a.getTicker())) return 1;
            if ("CASH".equals(b.getTicker())) return -1;
            return b.getDailyPnl().compareTo(a.getDailyPnl());
        });

        BigDecimal dayPnl = positions.stream()
            .map(PortfolioPosition::getDailyPnl)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPnl = positions.stream()
            .map(PortfolioPosition::getUnrealizedPnl)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal mktValue = positions.stream()
            .map(PortfolioPosition::getMktValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dayPnlPercent;
        if (mktValue.subtract(dayPnl).compareTo(BigDecimal.ZERO) == 0) {
            dayPnlPercent = BigDecimal.ZERO;
        } else {
            dayPnlPercent = dayPnl.multiply(BigDecimal.valueOf(100)).divide(mktValue.subtract(dayPnl), RoundingMode.HALF_EVEN);
        }
        BigDecimal netPnlPercent;
        if (mktValue.subtract(netPnl).compareTo(BigDecimal.ZERO) == 0) {
            netPnlPercent = BigDecimal.ZERO;
        } else {
            netPnlPercent = netPnl.multiply(BigDecimal.valueOf(100)).divide(mktValue.subtract(netPnl), RoundingMode.HALF_EVEN);
        }

        return EmailSnapshot.builder()
            .positions(positions)
            .positionCount(positions.size() - 1)
            .dayPnl(dayPnl)
            .dayPnlPercent(dayPnlPercent)
            .netPnl(netPnl)
            .netPnlPercent(netPnlPercent)
            .mktValue(mktValue)
            .build();
    }

    private <T> T http(String path, TypeReference<T> typeRef) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(portfolioEndpoint + path))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to fetch endpoint " + path);
            }
            return mapper.readValue(response.body(), typeRef);
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
