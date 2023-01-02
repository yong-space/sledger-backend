package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import tech.sledger.endpoints.AdminEndpoints;
import tech.sledger.model.account.AccountIssuer;
import javax.annotation.PostConstruct;
import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

public class AdminTests extends BaseTest {
    @PostConstruct
    public void init() {
        userConfig.setupUsers();
    }

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
        AccountIssuer accountIssuer = accountIssuerService.get("a");
        assertNotNull(accountIssuer);
        accountIssuerService.delete(accountIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void addMultipleIssuer() throws Exception {
        AdminEndpoints.NewAccountIssuer a1 = new AdminEndpoints.NewAccountIssuer("a1");
        AdminEndpoints.NewAccountIssuer a2 = new AdminEndpoints.NewAccountIssuer("a2");

        mvc.perform(request(POST, "/api/admin/account-issuer", a1))
            .andExpect(status().isOk());
        mvc.perform(request(POST, "/api/admin/account-issuer", a2))
            .andExpect(status().isOk());
        mvc.perform(get("/api/account-issuer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.id > 1)]").exists());
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void deleteIssuer() throws Exception {
        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer = accountIssuerService.add(accountIssuer);

        mvc.perform(delete("/api/admin/account-issuer/" + accountIssuer.getId()))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("basic-user@company.com")
    public void listIssuerSuccess() throws Exception {
        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("b");
        accountIssuer = accountIssuerService.add(accountIssuer);

        mvc.perform(get("/api/account-issuer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.name == 'b')]").exists());
        accountIssuerService.delete(accountIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void editIssuerSuccess() throws Exception {
        AccountIssuer accountIssuer = new AccountIssuer();
        accountIssuer.setName("c");
        accountIssuer = accountIssuerService.add(accountIssuer);

        accountIssuer.setName("c2");
        mvc.perform(request(PUT, "/api/admin/account-issuer", accountIssuer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("c2"));
        accountIssuerService.delete(accountIssuer);
    }

    @Test
    @WithUserDetails("admin-user@company.com")
    public void deleteMissingIssuer() throws Exception {
        mvc.perform(delete("/api/admin/account-issuer/99999"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("No such account issuer id"));
    }
}
