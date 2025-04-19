package tech.sledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.user.User;
import tech.sledger.repo.PortfolioSnapshotRepo;
import tech.sledger.service.ResendService;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PortfolioTests extends BaseTest {
    @MockitoBean
    PortfolioSnapshotRepo portfolioSnapshotRepo;
    @MockitoBean
    HttpClient httpClient;
    @MockitoBean
    private ResendService resendService;

    PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
        .id(1L)
        .owner(User.builder().id(1L).username("basic@sledger.tech").build())
        .cash(BigDecimal.valueOf(1_000))
        .holdings(BigDecimal.valueOf(5_000))
        .time(Instant.now())
        .build();

    @Test
    @WithUserDetails("basic-user@company.com")
    public void testAuthorisation() throws Exception {
        mvc.perform(get("/api/portfolio"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("basic-user2@company.com")
    public void getSnapshotExisting() throws Exception {
        when(portfolioSnapshotRepo.findFirstByOwner(any(User.class))).thenReturn(snapshot);

        mvc.perform(get("/api/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cash").value(1_000))
            .andExpect(jsonPath("$.holdings").value(5_000));
    }

    @Test
    @SuppressWarnings("unchecked")
    @WithUserDetails("basic-user2@company.com")
    public void getSnapshotWithRefresh() throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{ \"cash\": 1000, \"holdings\": 5000, \"fx\": 1.35 }");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);

        mvc.perform(get("/api/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cash").value(1_000))
            .andExpect(jsonPath("$.holdings").value(5_000));
    }

    @Test
    @SuppressWarnings("unchecked")
    @WithUserDetails("basic-user2@company.com")
    public void refreshSnapshotError() throws Exception {
        when(portfolioSnapshotRepo.findFirstByOwner(any(User.class))).thenReturn(snapshot);
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);
        mvc.perform(get("/api/portfolio/refresh"))
            .andExpect(status().isInternalServerError());

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(InterruptedException.class);
        mvc.perform(get("/api/portfolio/refresh"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @SuppressWarnings("unchecked")
    @WithUserDetails("basic-user2@company.com")
    public void emailSnapshot() throws Exception {
        String mockData = """
            [
            { "ticker": "CASH", "name": "Cash", "lastPrice": 0, "dailyPnl": 0, "changePercent": 0, "unrealizedPnl": 0, "unrealizedPnlPercent": 0, "mktValue": 200 },
            { "ticker": "A", "name": "A", "lastPrice": 150.00, "dailyPnl": 5.00, "changePercent": 0.03, "unrealizedPnl": 100.00, "unrealizedPnlPercent": 0.02, "mktValue": 5000.00 },
            { "ticker": "G", "name": "G", "lastPrice": 2800.00, "dailyPnl": -10.00, "changePercent": -0.004, "unrealizedPnl": -50.00, "unrealizedPnlPercent": -0.01, "mktValue": 2000.00 }
            ]
            """;
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(mockData);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);
        when(resendService.send(any(CreateEmailOptions.class)))
            .thenReturn(new CreateEmailResponse("abc"));

        mvc.perform(get("/api/portfolio/email-snapshot"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                Elements cells = Jsoup.parse(content).selectFirst("tr.summary-row").select("td");
                assertCell(cells.get(1), "-5", null, List.of("negative"));
                assertCell(cells.get(2), "-0.1%", null, List.of("negative"));
                assertCell(cells.get(3), "50", null, List.of("positive"));
                assertCell(cells.get(4), "0.7%", null, List.of("positive"));
                assertCell(cells.get(5), "7,200", null, Collections.emptyList());
            });
    }

    @Test
    @SuppressWarnings("unchecked")
    @WithUserDetails("basic-user2@company.com")
    public void emailSnapshotEdge() throws Exception {
        String mockData = """
            [
            { "ticker": "A", "name": "A", "lastPrice": 1.00, "dailyPnl": 1.00, "changePercent": 0, "unrealizedPnl": 1.00, "unrealizedPnlPercent": 0, "mktValue": 1.00 },
            { "ticker": "CASH", "name": "Cash", "lastPrice": 0, "dailyPnl": 0, "changePercent": 0, "unrealizedPnl": 0, "unrealizedPnlPercent": 0, "mktValue": 0 }
            ]
            """;
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(mockData);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);
        when(resendService.send(any(CreateEmailOptions.class)))
            .thenReturn(new CreateEmailResponse("abc"));

        mvc.perform(get("/api/portfolio/email-snapshot"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                Elements cells = Jsoup.parse(content).selectFirst("tr.summary-row").select("td");
                assertEquals(cells.get(2).text().trim(), "0.0%", "Cell text mismatch");
                assertEquals(cells.get(4).text().trim(), "0.0%", "Cell text mismatch");
            });
    }

    private void assertCell(Element cell, String text, String colspan, List<String> classes) {
        assertEquals(text, cell.text().trim(), "Cell text mismatch");
        assertEquals(Set.copyOf(classes), cell.classNames(), "CSS classes mismatch");
    }
}
