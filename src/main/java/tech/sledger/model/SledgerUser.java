package tech.sledger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Data
@Builder
public class SledgerUser implements UserDetails {
    @Id
    private long id;
    private String username;
    @JsonIgnore
    private String password;
    @JsonIgnore
    private boolean accountNonExpired = true;
    @JsonIgnore
    private boolean accountNonLocked = true;
    @JsonIgnore
    private boolean credentialsNonExpired = true;
    @JsonIgnore
    private boolean enabled = true;
    private Collection<? extends GrantedAuthority> authorities;
}
