package tech.sledger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.SledgerUser;
import tech.sledger.service.UserService;
import java.util.List;

@Service
public class UserConfig {
    @Autowired
    public UserService userService;
    SledgerUser basicUser = null;
    SledgerUser adminUser = null;

    public List<SledgerUser> setupUsers() {
        if (basicUser == null && adminUser == null) {
            basicUser = userService.add(SledgerUser.builder()
                .username("basic-user@company.com")
                .password("basic-user")
                .authorities(List.of())
                .build());
            adminUser = userService.add(SledgerUser.builder()
                .id(123456L)
                .username("admin-user@company.com")
                .password("admin-user")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build());
        }
        return List.of(basicUser, adminUser);
    }
}
