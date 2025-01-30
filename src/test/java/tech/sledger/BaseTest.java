package tech.sledger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
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
import org.testcontainers.junit.jupiter.Container;
import tech.sledger.model.user.User;
import tech.sledger.repo.AccountIssuerRepo;
import tech.sledger.repo.AccountRepo;
import tech.sledger.repo.ActivationRepo;
import tech.sledger.repo.TemplateRepo;
import tech.sledger.repo.TransactionRepo;
import tech.sledger.repo.UserRepo;
import tech.sledger.service.AccountIssuerService;
import tech.sledger.service.AccountService;
import tech.sledger.service.UserService;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    @Autowired
    public MockMvc mvc;
    @Autowired
    public ObjectMapper objectMapper;
    @Autowired
    public AccountIssuerService accountIssuerService;
    @Autowired
    public AccountIssuerRepo accountIssuerRepo;
    @Autowired
    public AccountService accountService;
    @Autowired
    public AccountRepo accountRepo;
    @Autowired
    public TransactionRepo txRepo;
    @Autowired
    public UserService userService;
    @Autowired
    public UserRepo userRepo;
    @Autowired
    public PasswordEncoder passwordEncoder;
    @Autowired
    public UserDetailsService userDetailsService;
    @Autowired
    public TemplateRepo templateRepo;
    @Autowired
    private ActivationRepo activationRepo;

    public enum SubmitMethod { PUT, POST }

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    static {
        System.setProperty("SLEDGER_SECRET_KEY", "my-secret-key");
        mongodb.start();
    }

    @BeforeAll
    public void initUsers() {
        userRepo.deleteAll();
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
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_TRADING")))
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

    @AfterEach
    void afterEach() {
        activationRepo.deleteAll();
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

    public Instant date(String date) {
        return ZonedDateTime.parse(date + "T00:00:00.000Z").toInstant();
    }
}
