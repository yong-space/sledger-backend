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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import tech.sledger.model.user.User;
import tech.sledger.repo.UserRepo;
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
    public UserRepo userRepo;
    @Autowired
    public PasswordEncoder passwordEncoder;
    @Autowired
    public UserDetailsService userDetailsService;

    enum SubmitMethod { PUT, POST }

    static {
        System.setProperty("SLEDGER_SECRET_KEY", "my-secret-key");
        System.setProperty("MONGO_URI", "mongodb://localhost/sledger");
    }

    @PostConstruct
    public void initUsers() {
        User u1 = userRepo.save(User.builder()
            .id(1)
            .displayName("Basic User 1")
            .username("basic-user@company.com")
            .password(passwordEncoder.encode("B4SicUs3r!"))
            .enabled(true)
            .build());
        User u2 = userRepo.save(User.builder()
            .id(2)
            .displayName("Basic User 2")
            .username("basic-user2@company.com")
            .password(passwordEncoder.encode("B4SicUs3r!"))
            .enabled(true)
            .build());
        User u3 = userRepo.save(User.builder()
            .id(3)
            .displayName("Admin User")
            .username("admin-user@company.com")
            .password(passwordEncoder.encode("aDm1nU53r$"))
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .enabled(true)
            .build());
        userRepo.saveAll(List.of(u1, u2, u3));
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
