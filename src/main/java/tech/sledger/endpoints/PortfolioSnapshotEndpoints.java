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
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.portfolio.PortfolioSummary;
import tech.sledger.model.user.User;
import tech.sledger.repo.PortfolioSnapshotRepo;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioSnapshotEndpoints {
    @Value("${portfolio-endpoint:http://portfolio}")
    public String portfolioEndpoint;
    public final PortfolioSnapshotRepo portfolioSnapshotRepo;
    public final ObjectMapper mapper;
    public final HttpClient httpClient;

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

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(portfolioEndpoint))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to fetch portfolio summary");
            }
            PortfolioSummary portfolio = mapper.readValue(response.body(), new TypeReference<>() {});
            snapshot.setCash(portfolio.getCash());
            snapshot.setHoldings(portfolio.getHoldings());
            snapshot.setFx(portfolio.getFx());
            snapshot.setTime(Instant.now());
            portfolioSnapshotRepo.save(snapshot);
            return snapshot;
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
