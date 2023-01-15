package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(profiles = "dev")
@SpringBootTest
@AutoConfigureMockMvc
public class DevCorsTests {
    @Autowired
    public MockMvc mvc;

    @Test
    public void cors() throws Exception {
        var request = options("/api/public/authenticate")
            .header("Access-Control-Request-Method", "POST")
            .header("Origin", "http://localhost:5173");
        mvc.perform(request)
            .andExpect(status().isOk());
    }
}
