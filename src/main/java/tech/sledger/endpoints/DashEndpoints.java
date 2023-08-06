package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.dto.Insight;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dash")
public class DashEndpoints {
  private final AccountRepo accountRepo;

  @GetMapping("insights")
  public List<Insight> getInsights(Authentication auth) {
    User user = (User) auth.getPrincipal();
    return accountRepo.getInsights(user.getId(), Instant.EPOCH);
  }
}
