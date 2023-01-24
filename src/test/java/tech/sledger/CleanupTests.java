package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.sledger.model.user.Activation;
import tech.sledger.model.user.User;
import tech.sledger.repo.ActivationRepo;
import tech.sledger.repo.UserRepo;
import tech.sledger.service.CleanupService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CleanupTests extends BaseTest {
    @Autowired
    private CleanupService cleanupService;
    @Autowired
    private ActivationRepo activationRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void cleanup() {
        String password = passwordEncoder.encode("T3stU52r!$");
        User u1 = User.builder().id(4).displayName("User A").username("a@a.com").password(password).build();
        User u2 = User.builder().id(5).displayName("User B").username("b@b.com").password(password).build();
        User u3 = User.builder().id(6).displayName("User C").username("c@c.com").password(password).build();
        userRepo.saveAll(List.of(u1, u2, u3));

        Activation a1 = Activation.builder().user(u1).code("u1").date(Instant.now().minus(5, ChronoUnit.DAYS)).build();
        Activation a2 = Activation.builder().user(u2).code("u2").date(Instant.now().minus(3, ChronoUnit.DAYS)).build();
        Activation a3 = Activation.builder().user(u3).code("u3").date(Instant.now().minus(1, ChronoUnit.DAYS)).build();
        activationRepo.saveAll(List.of(a1, a2, a3));

        cleanupService.cleanup();

        assertNull(userRepo.findFirstByUsername("a@a.com"));
        assertNull(userRepo.findFirstByUsername("b@b.com"));
        assertNotNull(userRepo.findFirstByUsername("c@c.com"));

        assertNull(activationRepo.findFirstByUser(u1));
        assertNull(activationRepo.findFirstByUser(u2));
        assertNotNull(activationRepo.findFirstByUser(u3));
    }
}
