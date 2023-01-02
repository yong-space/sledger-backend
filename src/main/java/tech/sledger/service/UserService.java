package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.sledger.model.SledgerUser;
import tech.sledger.repo.SledgerUserRepo;

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
}
