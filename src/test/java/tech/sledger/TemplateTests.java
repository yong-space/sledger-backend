package tech.sledger;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.security.test.context.support.WithUserDetails;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.BaseTest.SubmitMethod.POST;
import static tech.sledger.BaseTest.SubmitMethod.PUT;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateTests extends BaseTest {
    private static final AtomicLong id1 = new AtomicLong();
    private static final AtomicLong id2 = new AtomicLong();

    @Test
    @Order(1)
    @WithUserDetails("basic-user@company.com")
    public void addTemplate() throws Exception {
        Map<String, ?> template = Map.of(
        "reference", "my reference",
        "remarks", "my remarks",
        "category", "my category"
        );
        mvc.perform(request(POST, "/api/template", List.of(template)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[?(@.remarks == 'my remarks')]").exists())
            .andDo(res -> id1.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<List<Map>>(){}).get(0).get("id")));
    }

    @Test
    @Order(2)
    @WithUserDetails("admin-user@company.com")
    public void addOtherTemplate() throws Exception {
        Map<String, ?> template = Map.of(
            "reference", "my reference",
            "remarks", "my remarks",
            "category", "my category"
        );
        mvc.perform(request(POST, "/api/template", List.of(template)))
            .andExpect(status().isOk())
            .andDo(res -> id2.set((int) objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<List<Map>>(){}).get(0).get("id")));
    }

    @Test
    @Order(3)
    @WithUserDetails("basic-user@company.com")
    public void editTemplate() throws Exception {
        Map<String, ?> template = Map.of(
            "id", id1.get(),
            "reference", "my reference",
            "remarks", "modified remarks",
            "category", "my category"
        );
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
        mvc.perform(delete("/api/template/" + id1.get()))
            .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    @WithUserDetails("basic-user@company.com")
    public void badEditTemplate() throws Exception {
        Map<String, Object> template = new HashMap<>(Map.of(
            "id", 123,
            "reference", "my reference",
            "remarks", "modified remarks",
            "category", "my category"
        ));
        mvc.perform(request(PUT, "/api/template", List.of(template)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unable to find all templates requested"));

        template.put("id", id2.get());
        mvc.perform(request(PUT, "/api/template", List.of(template)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("You are not the owner of all templates requested"));
    }
}
