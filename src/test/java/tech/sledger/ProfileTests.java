package tech.sledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.PUT;
import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.model.user.Profile;

public class ProfileTests extends BaseTest {
    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNameWithoutPassword() throws Exception {
        Profile profile = new Profile("x", "basic-user@company.com", null, null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Display name should be between 2 and 20 characters"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileWithoutPassword() throws Exception {
        Profile profile = new Profile("Joe", "basic-user@company.com", null, null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        assertEquals("Joe", userService.get("basic-user@company.com").getDisplayName());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileUnnecessaryPassword() throws Exception {
        Profile profile = new Profile("Joe", "basic-user@company.com", "B4SicUs3rx", null);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        assertEquals("Joe", userService.get("basic-user@company.com").getDisplayName());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileMissingOldPassword() throws Exception {
        Profile profile = new Profile("Joe", "basic-user@company.com", null, "A@nb&nsckl2!");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Current password required to change password"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadOldPassword() throws Exception {
        Profile profile = new Profile("Joe", "basic-user@company.com", "B4SicUs3rx", "A@nb&nsckl2!");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid Credentials"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNewPassword() throws Exception {
        Profile profile = new Profile("Joe", "basic-user@company.com", "B4SicUs3r!", "A@nppppppppp");
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Password should contain at least 1 uppercase, 1 lowercase, 1 numeric and 1 special character"));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfilePassword() throws Exception {
        String newPassword = "A@nb&nsckl2!";
        Profile profile = new Profile("Joe", "basic-user@company.com", "B4SicUs3r!", newPassword);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isOk());
        Assertions.assertTrue(passwordEncoder.matches(newPassword, userService.get("basic-user@company.com").getPassword()));
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void updateProfileBadNamePassword() throws Exception {
        String newPassword = "A@nb&nsckl2!";
        Profile profile = new Profile("x", "basic-user@company.com", "B4SicUs3r!", newPassword);
        mvc.perform(request(PUT, "/api/profile", profile))
            .andExpect(status().isBadRequest());
    }
}
