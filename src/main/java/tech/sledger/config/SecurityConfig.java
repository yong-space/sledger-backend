package tech.sledger.config;

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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
            return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }
    }

    @Configuration
    @RequiredArgsConstructor
    static class AuthenticationConfig {
        private final UserDetailsService userDetailsService;
        private final PasswordEncoder passwordEncoder;

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService);
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
