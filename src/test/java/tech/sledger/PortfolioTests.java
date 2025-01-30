package tech.sledger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.user.User;
import tech.sledger.repo.PortfolioSnapshotRepo;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class PortfolioTests extends BaseTest {
    @MockitoBean
    PortfolioSnapshotRepo portfolioSnapshotRepo;
    @MockitoBean
    HttpClient httpClient;

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
}
