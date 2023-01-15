package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.User;
import tech.sledger.repo.UserRepo;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findFirstByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }
        return user;
    }
}
