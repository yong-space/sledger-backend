package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.user.Activation;
import tech.sledger.model.user.Profile;
import tech.sledger.model.user.Registration;
import tech.sledger.model.user.User;
import tech.sledger.repo.ActivationRepo;
import tech.sledger.repo.UserRepo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final ActivationRepo activationRepo;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    public Activation add(Registration registration) {
        String username = registration.getUsername().trim();
        User existing = userRepo.findFirstByUsername(username);
        if (existing != null) {
            if (!existing.isEnabled()) {
                throw new ResponseStatusException(BAD_REQUEST, "Account pending activation");
            }
            throw new ResponseStatusException(BAD_REQUEST, "Email already in use");
        }
        User previous = userRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;

        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        if (id == 1) {
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        User user = userRepo.save(User.builder()
            .id(id)
            .displayName(registration.getDisplayName().trim())
            .username(username)
            .password(passwordEncoder.encode(registration.getPassword().trim()))
            .authorities(roles)
            .build());

        Activation activation = Activation.builder()
            .user(user)
            .code(UUID.randomUUID().toString())
            .date(Instant.now())
            .build();
        activationRepo.save(activation);

        return activation;
    }

    public User edit(User user, Profile profile) {
        if (profile.getPassword() != null && profile.getNewPassword() != null) {
            if (!passwordEncoder.matches(profile.getPassword(), user.getPassword())) {
                throw new ResponseStatusException(UNAUTHORIZED, "Invalid Credentials");
            }
            user.setPassword(passwordEncoder.encode(profile.getNewPassword().trim()));
        }
        user.setUsername(profile.getUsername());
        user.setDisplayName(profile.getDisplayName());
        return userRepo.save(user);
    }

    public User get(String username) {
        return userRepo.findFirstByUsername(username.toLowerCase());
    }

    public void delete(User user) {
        userRepo.delete(user);
    }

    public Activation getActivation(String username) {
        return activationRepo.findFirstByUser(get(username));
    }

    public void activate(User user) {
        Activation activation = activationRepo.findFirstByUser(user);
        if (activation != null) {
            user.setEnabled(true);
            userRepo.save(user);
            activationRepo.delete(activation);
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "User has no pending activation");
        }
    }

    public void activate(String code) {
        Optional<Activation> activation = activationRepo.findById(code);
        if (activation.isPresent()) {
            User user = activation.get().getUser();
            user.setEnabled(true);
            userRepo.save(user);
            activationRepo.delete(activation.get());
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid activation code");
        }
    }

    @Cacheable(value="authorise", key="#accountId")
    public Account authorise(Authentication auth, long accountId) {
        Account account = accountService.get(accountId);
        if (account == null) {
            throw new ResponseStatusException(NOT_FOUND, "No such account id");
        }
        User user = (User) auth.getPrincipal();
        User owner = account.getOwner();
        if (user.getId() != owner.getId()) {
            throw new ResponseStatusException(UNAUTHORIZED, "You are not the owner of this account");
        }
        return account;
    }
}
