package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.sledger.endpoints.PublicEndpoints;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.User;
import tech.sledger.service.EmailService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static com.mongodb.assertions.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

public class UserTests extends BaseTest {
    @Autowired
    public PasswordEncoder passwordEncoder;
    @MockBean
    private EmailService emailService;

    @Test
    public void registerActivateSuccess() throws Exception {
        when(emailService.sendActivation(any(String.class), any(String.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        String username = "user@company.com";
        String password = "P4s5w0rdz!";
        Registration registration = new Registration("Display Name", username, password);
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        UserDetails user = userDetailsService.loadUserByUsername(username);
        Assertions.assertTrue(passwordEncoder.matches(password, user.getPassword()));

        User u1 = userService.get(username);
        assertNotNull(u1);

        String code = userService.getActivation(u1.getUsername()).getCode();
        mvc.perform(get("/api/public/activate/" + code))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    public void activateFail() throws Exception {
        mvc.perform(get("/api/public/activate/abc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void editDeleteUser() {
        String username = "disposable@company.com";
        userService.add(new Registration("Disposable", username, "M3hm3h!z%"));

        User user = userService.get(username);
        user.setDisplayName("Tom");
        userService.edit(user);
        assertTrue(userService.get(username).getDisplayName().equals("Tom"));

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
            .andExpect(jsonPath("$.detail").value("Username already exists"));
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
            .andDo(res -> jwt.set(objectMapper.readValue(res.getResponse().getContentAsString(), PublicEndpoints.TokenResponse.class).token()));

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
}
