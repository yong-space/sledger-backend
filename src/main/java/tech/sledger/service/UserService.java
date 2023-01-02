package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.sledger.model.SledgerUser;
import tech.sledger.repo.SledgerUserRepo;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final SledgerUserRepo userRepo;

    @Override
    public SledgerUser loadUserByUsername(String username) throws UsernameNotFoundException {
        SledgerUser user = userRepo.findFirstByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }
        return user;
    }

    public SledgerUser add(SledgerUser user) {
        SledgerUser previous = userRepo.findFirstByOrderByIdDesc();
        long id = (previous == null) ? 1 : previous.getId() + 1;
        user.setId(id);
        user.setPassword(passwordEncoder().encode(user.getPassword()));
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

    public void deleteAll() {
        userRepo.deleteAll();
    }

    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder();
    }
}
