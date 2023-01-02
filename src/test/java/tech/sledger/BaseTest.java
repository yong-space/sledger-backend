package tech.sledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import tech.sledger.service.UserService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class BaseTest {
    @Autowired
    public MockMvc mvc;
    @Autowired
    public ObjectMapper objectMapper;
    @Autowired
    public UserDetailsService userDetailsService;
    @Autowired
    public UserService userService;

    enum SubmitMethod { PUT, POST }

    @DynamicPropertySource
    @SuppressWarnings("resource")
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        MongoDBContainer container = new MongoDBContainer("mongo:5.0.8");
        container.start();
        registry.add("spring.data.mongodb.uri", container::getReplicaSetUrl);
    }

    public MockHttpServletRequestBuilder request(SubmitMethod method, String uri, Object payload) {
        try {
            MockHttpServletRequestBuilder builder = switch (method) {
                case PUT -> put(uri);
                case POST -> post(uri);
            };
            return builder.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
