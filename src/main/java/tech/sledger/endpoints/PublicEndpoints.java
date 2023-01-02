package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.JwtService;
import tech.sledger.service.UserService;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicEndpoints {
    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public record Credentials(String username, String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/register")
    public void register(@RequestBody Registration registration) {
        if (!registration.getPassword().trim().equals(registration.getPassword2().trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwords do not match");
        }
        SledgerUser user = SledgerUser.builder()
            .username(registration.getUsername().trim().toLowerCase())
            .password(registration.getPassword().trim())
            .build();
        userService.add(user);
    }

    @PostMapping("/authenticate")
    public TokenResponse authenticate(@RequestBody Credentials credentials) {
        String email = credentials.username().trim().toLowerCase();
        var token = new UsernamePasswordAuthenticationToken(email, credentials.password());
        try {
            Authentication auth = authManager.authenticate(token);
            SledgerUser user = (SledgerUser) auth.getPrincipal();
            String jwt = jwtService.generate(email, user);
            log.info("User logged in: {}", user.getUsername());
            return new TokenResponse(jwt);
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Credentials");
        }
    }
}
