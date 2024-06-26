package tech.sledger.endpoints;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.Activation;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.TokenResponse;
import tech.sledger.model.user.User;
import tech.sledger.service.EmailService;
import tech.sledger.service.JwtService;
import tech.sledger.service.UserService;
import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class UserEndpoints {
    private final UserService userService;
    private final EmailService emailService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public record Credentials(String username, String password) {}
    public record RegistrationResponse(String status) {}

    @PostMapping("register")
    public RegistrationResponse register(
        @RequestBody @Valid Registration registration,
        BindingResult binding
    ) {
        if (binding.hasErrors()) {
            throw new ResponseStatusException(BAD_REQUEST, binding.getFieldError().getDefaultMessage());
        }
        Activation activation = userService.add(registration);
        User user = activation.getUser();
        emailService.sendActivation(user.getUsername(), user.getDisplayName(), activation.getCode());
        log.info("New Registration: {}", user.getUsername());
        return new RegistrationResponse("ok");
    }

    @GetMapping("activate/{code}")
    public void activate(@PathVariable("code") String code, HttpServletResponse response) throws IOException {
        userService.activate(code);
        response.sendRedirect("/login#activated");
    }

    @PostMapping("authenticate")
    public TokenResponse authenticate(@RequestBody Credentials credentials) {
        if (credentials.username == null || credentials.password == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid Credentials");
        }
        String email = credentials.username().trim().toLowerCase();
        var token = new UsernamePasswordAuthenticationToken(email, credentials.password().trim());
        try {
            Authentication auth = authManager.authenticate(token);
            User user = (User) auth.getPrincipal();
            String jwt = jwtService.generate(user);
            log.info("User logged in: {}", user.getUsername());
            return new TokenResponse(jwt);
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Credentials");
        }
    }

    @GetMapping("refresh-token")
    public TokenResponse refreshToken(Authentication auth) {
        User user = (User) auth.getPrincipal();
        String jwt = jwtService.generate(user);
        log.info("User token refreshed: {}", user.getUsername());
        return new TokenResponse(jwt);
    }
}
