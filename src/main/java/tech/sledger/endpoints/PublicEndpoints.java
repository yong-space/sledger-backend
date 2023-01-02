package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.UserService;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicEndpoints {
    private final UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody Registration registration) {
        if (!registration.getPassword().trim().equals(registration.getPassword2().trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwords do not match");
        }
        SledgerUser user = SledgerUser.builder()
            .username(registration.getUsername().trim().toLowerCase())
            .password(registration.getPassword().trim())
            .build();
        try {
            userService.add(user);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }
}
