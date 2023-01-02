package tech.sledger.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SledgerUser implements UserDetails {
    @Id
    private long id;
    private String displayName;
    private String username;
    @JsonIgnore
    private String password;
    @JsonIgnore
    @Builder.Default
    private boolean accountNonExpired = true;
    @JsonIgnore
    @Builder.Default
    private boolean accountNonLocked = true;
    @JsonIgnore
    @Builder.Default
    private boolean credentialsNonExpired = true;
    @JsonIgnore
    @Builder.Default
    private boolean enabled = true;
    private Collection<? extends GrantedAuthority> authorities;
}
