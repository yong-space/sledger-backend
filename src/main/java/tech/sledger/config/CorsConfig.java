package tech.sledger.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {
    @Value("${sledger.cors:false}")
    private String cors;

    @Bean
    public CorsFilter corsFilter() {
        var source = new UrlBasedCorsConfigurationSource();
        if ("true".equalsIgnoreCase(cors)) {
            log.info("CorsFilter Activated");
            var config = new CorsConfiguration();
            config.addAllowedOrigin("*");
            config.addAllowedHeader("*");
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            source.registerCorsConfiguration("/**", config);
        }
        return new CorsFilter(source);
    }
}
