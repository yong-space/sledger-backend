package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.sledger.model.user.Registration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

public class UserTests extends BaseTest {
    @Autowired
    public PasswordEncoder passwordEncoder;

    @Test
    public void registerMismatchedPasswords() throws Exception {
        Registration registration = new Registration("u0", "p1", "p2");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Passwords do not match"));
    }

    @Test
    public void registerSuccess() throws Exception {
        Registration registration = new Registration("u1", "p1", "p1");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        UserDetails u = userDetailsService.loadUserByUsername("u1");
        Assertions.assertTrue(passwordEncoder.matches("p1", u.getPassword()));
    }

    @Test
    public void registerUsernameExists() throws Exception {
        Registration registration = new Registration("u2", "p1", "p1");
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Username already exists"));
    }
}
