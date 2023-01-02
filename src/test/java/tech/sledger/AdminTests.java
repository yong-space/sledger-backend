package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import tech.sledger.endpoints.AdminEndpoints;
import tech.sledger.repo.AccountIssuerRepo;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(UserConfig.class)
@ActiveProfiles("in-memory-uds")
public class AdminTests extends BaseTest {
    @Autowired
    public AccountIssuerRepo accountIssuerRepo;

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addIssuerForbidden() throws Exception {
        AdminEndpoints.NewAccountIssuer issuer = new AdminEndpoints.NewAccountIssuer("a");
        mvc.perform(postRequest("/api/admin/account-issuer", issuer))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void addIssuerSuccess() throws Exception {
        AdminEndpoints.NewAccountIssuer issuer = new AdminEndpoints.NewAccountIssuer("a");
        mvc.perform(postRequest("/api/admin/account-issuer", issuer))
            .andExpect(status().isOk());
        assertNotNull(accountIssuerRepo.findFirstByName("a"));
    }
}
