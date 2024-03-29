package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.dto.CategorySuggestion;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountRepo;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/suggest")
public class SuggestEndpoints {
    private final AccountRepo accountRepo;

    @GetMapping("/remarks")
    public List<String> suggestRemarks(Authentication auth, @RequestParam("q") String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "remarks", cleanQuery(q));
    }

    @GetMapping("/code")
    public List<String> suggestCode(Authentication auth, @RequestParam("q") String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "code", cleanQuery(q));
    }

    @GetMapping("/company")
    public List<String> suggestCompany(Authentication auth, @RequestParam("q") String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "company", cleanQuery(q));
    }

    @GetMapping("/categories")
    public List<CategorySuggestion> getCategories(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getCategories(user.getId());
    }

    private String cleanQuery(String raw) {
        return Pattern.quote(raw);
    }
}
