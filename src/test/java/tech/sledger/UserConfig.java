package tech.sledger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.repo.SledgerUserRepo;
import java.util.List;

@Service
public class UserConfig {
    @Autowired
    public SledgerUserRepo userRepo;

    public void setupUsers() {
        SledgerUser basicUser = SledgerUser.builder()
            .id(12345L)
            .username("basic-user@company.com")
            .password("basic-user")
            .authorities(List.of())
            .build();
        SledgerUser adminUser = SledgerUser.builder()
            .id(123456L)
            .username("admin-user@company.com")
            .password("admin-user")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build();
        userRepo.saveAll(List.of(basicUser, adminUser));
    }
}
