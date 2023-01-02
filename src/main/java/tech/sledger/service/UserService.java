package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.repo.SledgerUserRepo;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final SledgerUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    public SledgerUser add(SledgerUser user) {
        SledgerUser existing = userRepo.findFirstByUsername(user.getUsername());
        if (existing != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
        }

        SledgerUser previous = userRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        user.setId(id);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public SledgerUser get(String username) {
        return userRepo.findFirstByUsername(username.toLowerCase());
    }

    public List<SledgerUser> list() {
        return userRepo.findAll();
    }

    public SledgerUser save(SledgerUser user) {
        return userRepo.save(user);
    }

    public void delete(SledgerUser user) {
        userRepo.delete(user);
    }

    public Account authorise(Authentication auth, long accountId) {
        Account account = accountService.get(accountId);
        if (account == null) {
            throw new ResponseStatusException(NOT_FOUND, "No such account id");
        }
        SledgerUser user = (SledgerUser) auth.getPrincipal();
        SledgerUser owner = account.getOwner();
        if (user.getId() != owner.getId()) {
            throw new ResponseStatusException(UNAUTHORIZED, "You are not the owner of this account");
        }
        return account;
    }
}
