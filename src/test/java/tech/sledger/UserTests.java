package tech.sledger;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.TokenResponse;
import tech.sledger.model.user.User;
import tech.sledger.service.EmailService;
import tech.sledger.service.JwtService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import static com.mongodb.assertions.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

public class UserTests extends BaseTest {
    @MockBean
    private EmailService emailService;
    @Autowired
    private JwtService jwtService;

    @Test
    public void activateFail() throws Exception {
        mvc.perform(get("/api/public/activate/abc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteUser() {
        String username = "disposable@company.com";
        userService.add(new Registration("Disposable", username, "M3hm3h!z%"));

        User user = userService.get(username);
        userService.delete(user);
        assertTrue(userService.get(username) == null);
    }

    @Test
    public void registerUsernameExists() throws Exception {
        when(emailService.sendActivation(any(String.class), any(String.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        Registration registration = new Registration("Duplicate", "duplicate@company.com", "M3hm3h!z%");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Account pending activation"));

        User user = userService.get("duplicate@company.com");
        userService.activate(user);

        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Email already in use"));
    }

    @Test
    public void missingActivation() {
        User user = userService.get("basic-user@company.com");
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> userService.activate(user));
        assertEquals("User has no pending activation", thrown.getReason());
    }

    @Test
    public void registerBadPassword() throws Exception {
        Registration registration = new Registration("Validate", "validate@company.com", "A1a@!");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Password should be between 8 and 100 characters"));
    }

    @Test
    public void loginNoCredentials() throws Exception {
        mvc.perform(request(POST, "/api/public/authenticate", Map.of("username", "u")))
            .andExpect(status().isBadRequest());
        mvc.perform(request(POST, "/api/public/authenticate", Map.of("password", "p")))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void loginBadCredentials() throws Exception {
        mvc.perform(request(POST, "/api/public/authenticate", Map.of("username", "a", "password", "b")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginSuccess() throws Exception {
        String username = "basic-user@company.com";
        String password = "B4SicUs3r!";
        AtomicReference<String> jwt = new AtomicReference<>();
        mvc.perform(request(POST, "/api/public/authenticate", Map.of("username", username, "password", password)))
            .andExpect(status().isOk())
            .andDo(res -> jwt.set(objectMapper.readValue(res.getResponse().getContentAsString(), TokenResponse.class).token()));

        mvc.perform(get("/api/account").header("Authorization", "Bearer " + jwt.get()))
            .andExpect(status().isOk());
    }

    @Test
    public void badJwt() throws Exception {
        mvc.perform(get("/api/account"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/account").header("Authorization", ""))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/account").header("Authorization", "Bearer xyz"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void jwtRoleTest() {
        User basicUser = userService.get("basic-user@company.com");
        User adminUser = userService.get("admin-user@company.com");
        DecodedJWT basicJwt = jwtService.validate(jwtService.generate(basicUser));
        DecodedJWT adminJwt = jwtService.validate(jwtService.generate(adminUser));
        basicUser.setAuthorities(List.of());
        DecodedJWT basicJwt2 = jwtService.validate(jwtService.generate(basicUser));
        assertEquals("false", basicJwt.getClaims().get("admin").toString());
        assertEquals("false", basicJwt2.getClaims().get("admin").toString());
        assertEquals("true", adminJwt.getClaims().get("admin").toString());
    }
}
