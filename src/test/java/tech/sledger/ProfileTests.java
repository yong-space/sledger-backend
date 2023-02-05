package tech.sledger;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.user.Profile;
import tech.sledger.model.user.TokenResponse;
import tech.sledger.service.JwtService;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class ProfileTests extends BaseTest {
    @Autowired
    private JwtService jwtService;

    @Test
    @WithUserDetails("basic-user@company.com")
    public void getProfile() throws Exception {
        AtomicReference<String> jwt = new AtomicReference<>();
        mvc.perform(get("/api/profile"))
            .andExpect(status().isOk())
            .andDo(res -> jwt.set(objectMapper.readValue(res.getResponse().getContentAsString(), TokenResponse.class).token()));
        jwtService.validate(jwt.get());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNameWithoutPassword() throws Exception {
        Profile profile = new Profile("x", "joe@dough.com", null, null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Display name should be between 2 and 20 characters"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileWithoutPassword() throws Exception {
        Profile profile = new Profile("Joe", "joe@dough.com", null, null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        assertEquals("Joe", userService.get("joe@dough.com").getDisplayName());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileUnnecessaryPassword() throws Exception {
        Profile profile = new Profile("Joe", "joe@dough.com", "B4SicUs3rx", null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        assertEquals("Joe", userService.get("joe@dough.com").getDisplayName());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileMissingOldPassword() throws Exception {
        Profile profile = new Profile("Joe", "joe@dough.com", null, "A@nb&nsckl2!");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Current password required to change password"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadOldPassword() throws Exception {
        Profile profile = new Profile("Joe", "joe@dough.com", "B4SicUs3rx", "A@nb&nsckl2!");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid Credentials"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNewPassword() throws Exception {
        Profile profile = new Profile("Joe", "joe@dough.com", "B4SicUs3r!", "A@nppppppppp");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Password should contain at least 1 uppercase, 1 lowercase, 1 numeric and 1 special character"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfilePassword() throws Exception {
        String newPassword = "A@nb&nsckl2!";
        Profile profile = new Profile("Joe", "joe@dough.com", "B4SicUs3r!", newPassword);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        Assertions.assertTrue(passwordEncoder.matches(newPassword, userService.get("joe@dough.com").getPassword()));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNamePassword() throws Exception {
        String newPassword = "A@nb&nsckl2!";
        Profile profile = new Profile("x", "joe@dough.com", "B4SicUs3r!", newPassword);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest());
    }
}
