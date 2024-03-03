package tech.sledger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles(profiles = "dev")
public class DevCorsTests extends BaseTest {
    @Autowired
    public MockMvc mvc;

    @Test
    public void cors() throws Exception {
        var request = options("/api/authenticate")
            .header("Access-Control-Request-Method", "POST")
            .header("Origin", "http://localhost:5173");
        mvc.perform(request)
            .andExpect(status().isOk());
    }
}
