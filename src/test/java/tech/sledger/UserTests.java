package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.sledger.endpoints.PublicEndpoints;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.User;
import java.util.concurrent.atomic.AtomicReference;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static com.mongodb.assertions.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

public class UserTests extends BaseTest {
    @Autowired
    public PasswordEncoder passwordEncoder;

    @Test
    public void registerSuccess() throws Exception {
        String username = "user@company.com";
        String password = "P4s5w0rdz!";
        Registration registration = new Registration("Display Name", username, password);
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        UserDetails user = userDetailsService.loadUserByUsername(username);
        Assertions.assertTrue(passwordEncoder.matches(password, user.getPassword()));

        User u1 = userService.list().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
        assertNotNull(u1);
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
        Registration registration = new Registration("Duplicate", "duplicate@company.com", "M3hm3h!z%");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Username already exists"));
    }

    @Test
    public void loginBadCredentials() throws Exception {
        PublicEndpoints.Credentials credentials = new PublicEndpoints.Credentials("a", "b");
        mvc.perform(request(POST, "/api/public/authenticate", credentials))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginSuccess() throws Exception {
        String username = "basic-user@company.com";
        AtomicReference<String> jwt = new AtomicReference<>();
        PublicEndpoints.Credentials credentials = new PublicEndpoints.Credentials(username, "B4SicUs3r!");
        mvc.perform(request(POST, "/api/public/authenticate", credentials))
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
