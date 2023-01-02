package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.SledgerUser;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebService {
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService txService;

    public record Registration(String username, String password, String password2) {}

    @PostMapping("/api/public/register")
    public void register(@RequestBody Registration registration) {
        if (!registration.password.trim().equals(registration.password2.trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwords do not match");
        }
        SledgerUser user = SledgerUser.builder()
            .username(registration.username.trim().toLowerCase())
            .password(registration.password.trim())
            .build();
        try {
            userService.add(user);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }
}
