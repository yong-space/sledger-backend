package tech.sledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.TransactionService;
import tech.sledger.service.UserService;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class BaseTest {
    @Autowired
    public MockMvc mvc;
    @Autowired
    public ObjectMapper objectMapper;
    @Autowired
    public AccountIssuerService accountIssuerService;
    @Autowired
    public AccountService accountService;
    @Autowired
    public TransactionService transactionService;
    @Autowired
    public UserService userService;
    @Autowired
    public UserDetailsService userDetailsService;

    enum SubmitMethod { PUT, POST }

    @PostConstruct
    public void initUsers() {
        if (userService.get("basic-user@company.com") != null) {
            return;
        }
        userService.add(SledgerUser.builder()
            .id(123455L)
            .username("basic-user@company.com")
            .password("basic-user")
            .authorities(List.of())
            .build());
        userService.add(SledgerUser.builder()
            .id(123456L)
            .username("admin-user@company.com")
            .password("admin-user")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build());
    }

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
