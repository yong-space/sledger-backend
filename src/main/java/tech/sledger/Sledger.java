package tech.sledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import tech.sledger.config.AotHints;

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@ImportRuntimeHints(AotHints.class)
public class Sledger {
    public static void main(String[] args) {
        SpringApplication.run(Sledger.class, args);
    }
}
