package tech.sledger.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder.serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(
                BigDecimal value,
                JsonGenerator gen,
                SerializerProvider serializers
            ) throws IOException {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_EVEN).stripTrailingZeros());
            }
        });
    }
}
