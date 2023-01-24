package tech.sledger.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtService {
    @Value("${sledger.secret-key}")
    private String secretKey;
    private GrantedAuthority adminRole = new SimpleGrantedAuthority("ROLE_ADMIN");

    public DecodedJWT validate(String token) {
        return JWT.require(Algorithm.HMAC512(secretKey))
            .build()
            .verify(token);
    }

    public String generate(User user) {
        Instant now = Instant.now();
        boolean isAdmin = user.getAuthorities() != null && user.getAuthorities().contains(adminRole);
        return JWT.create()
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(1L, ChronoUnit.DAYS)))
            .withSubject(user.getUsername())
            .withClaim("name", user.getDisplayName())
            .withClaim("admin", isAdmin)
            .sign(Algorithm.HMAC512(secretKey));
    }
}
