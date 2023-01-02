package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.repo.SledgerUserRepo;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final SledgerUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public SledgerUser add(SledgerUser user) throws Exception {
        SledgerUser existing = userRepo.findFirstByUsername(user.getUsername());
        if (existing != null) {
            throw new Exception("Username already exists");
        }

        SledgerUser previous = userRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        user.setId(id);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
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
}
