package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.User;
import tech.sledger.service.EmailService;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;

public class UserBaseTest extends BaseTest {
    @Autowired
    public PasswordEncoder passwordEncoder;
    @MockBean
    private EmailService emailService;

    @Test
    public void registerActivateSuccess() throws Exception {
        userRepo.deleteAll();

        when(emailService.sendActivation(any(String.class), any(String.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        String username = "user@company.com";
        String password = "P4s5w0rdz!";
        Registration registration = new Registration("Display Name", username, password);
        mvc.perform(request(POST, "/api/public/register", registration))
            .andExpect(status().isOk());

        assertEquals(1, userService.get("user@company.com").getId());

        UserDetails user = userDetailsService.loadUserByUsername(username);
        Assertions.assertTrue(passwordEncoder.matches(password, user.getPassword()));

        User u1 = userService.get(username);
        assertNotNull(u1);

        String code = userService.getActivation(u1.getUsername()).getCode();
        mvc.perform(get("/api/public/activate/" + code))
            .andExpect(status().is3xxRedirection());
    }
}
