package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data")
public class DataEndpoints {
    private final AccountRepo accountRepo;

    @GetMapping("/suggest-remarks")
    public List<String> suggestRemarks(Authentication auth, @RequestParam String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopRemarks(user.getId(), q);
    }
}
