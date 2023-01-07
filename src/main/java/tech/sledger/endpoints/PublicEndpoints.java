package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
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
    public record RegistrationResponse(String status) {}

    @PostMapping("/register")
    public RegistrationResponse register(@RequestBody Registration registration) {
        if (
            registration.getDisplayName() == null ||
            registration.getUsername() == null ||
            registration.getPassword() == null ||
            registration.getPassword2() == null
        ) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid Registration");
        }
        if (!registration.getPassword().trim().equals(registration.getPassword2().trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwords do not match");
        }
        SledgerUser user = userService.add(SledgerUser.builder()
            .displayName(registration.getDisplayName().trim())
            .username(registration.getUsername().trim().toLowerCase())
            .password(registration.getPassword().trim())
            .build());
        log.info("New Registration: {}", user.getUsername());
        return new RegistrationResponse("ok");
    }

    @PostMapping("/authenticate")
    public TokenResponse authenticate(@RequestBody Credentials credentials) {
        if (credentials.username == null || credentials.password == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Credentials");
        }
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

    @GetMapping("/invalid")
    public String invalid() {
        throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT);
    }
}
