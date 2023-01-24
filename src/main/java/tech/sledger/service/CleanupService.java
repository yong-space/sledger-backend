package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.Activation;
import tech.sledger.repo.ActivationRepo;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {
    private final ActivationRepo activationRepo;
    private final UserService userService;

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanup() {
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        List<Activation> activations = activationRepo.findAllByDateBefore(twoDaysAgo);

        if (activations.isEmpty()) {
            return;
        }
        log.info("Registration cleanup initiated");

        activations.stream()
            .map(Activation::getUser)
            .forEach(u -> {
                userService.delete(u);
                log.info("Deleted user account: {}", u.getUsername());
            });
        activationRepo.deleteAll(activations);
        log.info("Cleanup complete");
    }
}
