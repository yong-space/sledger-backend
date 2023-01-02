package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.endpoints.AdminEndpoints;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.repo.AccountIssuerRepo;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

@Import(UserConfig.class)
public class AdminTests extends BaseTest {
    @Autowired
    public AccountIssuerRepo accountIssuerRepo;

    @Test
    @WithUserDetails("basic-user@company.com")
    public void addIssuerForbidden() throws Exception {
        AdminEndpoints.NewAccountIssuer issuer = new AdminEndpoints.NewAccountIssuer("a");
        mvc.perform(request(POST, "/api/admin/account-issuer", issuer))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void addIssuerSuccess() throws Exception {
        AdminEndpoints.NewAccountIssuer issuer = new AdminEndpoints.NewAccountIssuer("a");
        mvc.perform(request(POST, "/api/admin/account-issuer", issuer))
            .andExpect(status().isOk());
        AccountIssuer accountIssuer = accountIssuerRepo.findFirstByName("a");
        assertNotNull(accountIssuer);
        accountIssuerRepo.delete(accountIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void addMultipleIssuerAndDeleteSuccess() throws Exception {
        AdminEndpoints.NewAccountIssuer a1 = new AdminEndpoints.NewAccountIssuer("a1");
        AdminEndpoints.NewAccountIssuer a2 = new AdminEndpoints.NewAccountIssuer("a2");

        mvc.perform(request(POST, "/api/admin/account-issuer", a1))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/admin/account-issuer", a2))
            .andExpect(status().isOk());
        mvc.perform(get("/api/account-issuer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[1].id").value(2L));
        mvc.perform(delete("/api/admin/account-issuer/1"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/account-issuer/2"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void listIssuerSuccess() throws Exception {
        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer.setId(1);
        accountIssuerRepo.save(accountIssuer);

        mvc.perform(get("/api/account-issuer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].name").value("b"));
        accountIssuerRepo.delete(accountIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void editIssuerSuccess() throws Exception {
        AdminEndpoints.NewAccountIssuer issuer = new AdminEndpoints.NewAccountIssuer("c");
        mvc.perform(request(POST, "/api/admin/account-issuer", issuer))
            .andExpect(status().isOk());
        AccountIssuer editedIssuer = new AccountIssuer();
        editedIssuer.setName("c2");
        editedIssuer.setId(1);

        mvc.perform(request(PUT, "/api/admin/account-issuer", editedIssuer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("c2"));
        accountIssuerRepo.delete(editedIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void deleteMissingIssuer() throws Exception {
        mvc.perform(delete("/api/admin/account-issuer/99999"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("No such account issuer id"));
    }
}
