package tech.sledger.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
public class JacksonConfig {
    @Bean
    public JsonMapperBuilderCustomizer customizer() {
        return builder -> builder.addModule(new SimpleModule().addSerializer(BigDecimal.class, new ValueSerializer<>() {
            @Override
            public void serialize(
                BigDecimal value,
                JsonGenerator gen,
                SerializationContext serializers
            ) throws JacksonException {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_EVEN).stripTrailingZeros());
            }
        }));
    }
}
