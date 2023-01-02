package tech.sledger.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    @Value("${sledger.secret-key}")
    private String secretKey;
    private final UserDetailsService userDetailsService;

    @Override

    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String jwt = authHeader.substring(7);
                DecodedJWT token = validate(jwt);
                String username = token.getSubject();
                UserDetails user = userDetailsService.loadUserByUsername(username);
                Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
                var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                response.setStatus(401);
            }
        }
        chain.doFilter(request, response);
    }

    public DecodedJWT validate(String token) {
        return JWT.require(Algorithm.HMAC512(secretKey))
            .build()
            .verify(token);
    }
}
