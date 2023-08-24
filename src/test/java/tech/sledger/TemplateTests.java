package tech.sledger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.security.test.context.support.WithUserDetails;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateTests extends BaseTest {
    private static int id1;
    private static int id2;

    Map<String, Object> template = new HashMap<>(Map.of(
        "reference", "my reference",
        "remarks", "my remarks",
        "category", "my category",
        "subCategory", "my sub-category"
    ));

    @Test
    @Order(1)
    @WithUserDetails("basic-user@company.com")
    public void addTemplate() throws Exception {
        String result = mvc.perform(request(POST, "/api/template", List.of(template)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.remarks == 'my remarks')]").exists())
            .andReturn().getResponse().getContentAsString();
        id1 = JsonPath.parse(result).read("$.[0].id");
    }

    @Test
    @Order(2)
    @WithUserDetails("admin-user@company.com")
    public void addOtherTemplate() throws Exception {
        String result = mvc.perform(request(POST, "/api/template", List.of(template)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        id2 = JsonPath.parse(result).read("$.[0].id");
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void editTemplate() throws Exception {
        template.put("id", id1);
        template.put("remarks", "modified remarks");
        mvc.perform(request(PUT, "/api/template", List.of(template)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.remarks == 'modified remarks')]").exists());
    }

    @Test
    @Order(4)
    @WithUserDetails("basic-user@company.com")
    public void listTemplates() throws Exception {
        mvc.perform(get("/api/template"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.remarks == 'modified remarks')]").exists());
    }

    @Test
    @Order(5)
    @WithUserDetails("basic-user@company.com")
    public void deleteTemplate() throws Exception {
        mvc.perform(delete("/api/template/" + id1))
            .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    @WithUserDetails("basic-user@company.com")
    public void badEditTemplate() throws Exception {
        template.put("id", 123);

        mvc.perform(request(PUT, "/api/template", List.of(template)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unable to find all templates requested"));

        template.put("id", id2);
        mvc.perform(request(PUT, "/api/template", List.of(template)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("You are not the owner of all templates requested"));
    }
}
