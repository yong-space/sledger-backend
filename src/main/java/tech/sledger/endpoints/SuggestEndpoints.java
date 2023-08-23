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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/suggest")
public class SuggestEndpoints {
    private final AccountRepo accountRepo;

    @GetMapping("/remarks")
    public List<String> suggestRemarks(Authentication auth, @RequestParam String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "remarks", q);
    }

    @GetMapping("/category")
    public List<String> suggestCategory(Authentication auth, @RequestParam String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "category", q);
    }

    @GetMapping("/code")
    public List<String> suggestCode(Authentication auth, @RequestParam String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "code", q);
    }

    @GetMapping("/company")
    public List<String> suggestCompany(Authentication auth, @RequestParam String q) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getTopStrings(user.getId(), "company", q);
    }

    @GetMapping("/categories")
    public List<CategorySuggestion> getCategories(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return accountRepo.getCategories(user.getId());
    }
}
