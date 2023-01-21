package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MiscTests {
    @Autowired
    public MockMvc mvc;

    static {
        System.setProperty("SLEDGER_SECRET_KEY", "my-secret-key");
        System.setProperty("MONGO_URI", "mongodb://localhost/sledger");
    }

    @Test
    public void invalid() throws Exception {
        mvc.perform(get( "/api/public/invalid"))
            .andExpect(status().isIAmATeapot());
    }
}
