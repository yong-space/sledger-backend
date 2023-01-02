package tech.sledger.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.sledger.model.user.SledgerUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtService {
    @Value("${sledger.secret-key}")
    private String secretKey;

    public DecodedJWT validate(String token) {
        return JWT.require(Algorithm.HMAC512(secretKey))
            .build()
            .verify(token);
    }

    public String generate(String username, SledgerUser user) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(1L, ChronoUnit.DAYS)))
            .withSubject(username)
            .withClaim("name", user.getDisplayName())
            .sign(Algorithm.HMAC512(secretKey));
    }
}
