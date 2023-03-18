package tech.sledger.endpoints;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.Profile;
import tech.sledger.model.user.TokenResponse;
import tech.sledger.model.user.User;
import tech.sledger.service.JwtService;
import tech.sledger.service.UserService;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileEndpoints {
    private final UserService userService;
    private final JwtService jwtService;

    @PutMapping
    public TokenResponse updateProfile(
        Authentication auth,
        @RequestBody @Valid Profile profile,
        BindingResult binding
    ) {
        if (binding.hasErrors()) {
            List<String> fields = binding.getFieldErrors().stream().map(FieldError::getField).toList();
            if (fields.contains("newPassword") && profile.getNewPassword() == null) {
                fields = fields.stream().filter(f -> !f.equals("newPassword")).toList();
            }
            if (!fields.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, binding.getFieldError(fields.get(0)).getDefaultMessage());
            }
        }
        if (profile.getPassword() == null && profile.getNewPassword() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Current password required to change password");
        }
        User user = userService.edit((User) auth.getPrincipal(), profile);
        String jwt = jwtService.generate(user);
        return new TokenResponse(jwt);
    }
}
