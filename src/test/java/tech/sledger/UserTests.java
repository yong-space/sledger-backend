package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.WebService.Registration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserTests extends BaseTest {
    @Test
    public void registerMismatchedPasswords() throws Exception {
        Registration registration = new Registration("u0", "p1", "p2");
        mvc.perform(postRequest("/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Passwords do not match"));
    }

    @Test
    public void registerSuccess() throws Exception {
        Registration registration = new Registration("u1", "p1", "p1");
        mvc.perform(postRequest("/api/public/register", registration))
            .andExpect(status().isOk());
        SledgerUser u = userService.loadUserByUsername("u1");
        Assertions.assertTrue(userService.passwordEncoder().matches("p1", u.getPassword()));
    }

    @Test
    public void registerUsernameExists() throws Exception {
        Registration registration = new Registration("u2", "p1", "p1");
        mvc.perform(postRequest("/api/public/register", registration))
            .andExpect(status().isOk());
        mvc.perform(postRequest("/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Username already exists"));
    }
}
