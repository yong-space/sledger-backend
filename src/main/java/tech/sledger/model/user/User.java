package tech.sledger.model.user;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class User implements UserDetails {
    @Id
    private long id;
    private String displayName;
    @Indexed(unique = true)
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
    private boolean enabled;
    @JsonIgnore
    private Collection<SimpleGrantedAuthority> authorities;
}
