package tech.sledger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import tech.sledger.model.user.SledgerUser;
import java.util.List;

@TestConfiguration
public class UserConfig {
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
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
        return new InMemoryUserDetailsManager(List.of(basicUser, adminUser));
    }
}
