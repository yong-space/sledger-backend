package tech.sledger.config;

import com.password4j.Argon2Function;
import com.password4j.types.Argon2;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthEntryPoint entryPoint;
    private final JwtRequestFilter jwtRequestFilter;
    private final DaoAuthenticationProvider authenticationProvider;
    private final String[] publicEndpoints = {
        "/actuator/**", "/api/register", "/api/activate/**", "/api/authenticate", "/error", "/api/profile/challenge"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(publicEndpoints).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/portfolio/**").hasRole("TRADING")
                .requestMatchers("/api/**").authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
            .build();
    }

    @Configuration
    static class EncoderConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            var args = Argon2Function.getInstance(1 << 14, 2, 1, 32, Argon2.ID);
            return new Argon2Password4jPasswordEncoder(args);
        }
    }

    @Configuration
    @RequiredArgsConstructor
    static class AuthenticationConfig {
        private final UserDetailsService userDetailsService;
        private final PasswordEncoder passwordEncoder;

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
            authProvider.setPasswordEncoder(passwordEncoder);
            return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfiguration
        ) throws Exception {
            return authConfiguration.getAuthenticationManager();
        }
    }

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
