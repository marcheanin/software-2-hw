package ru.mipt.software2.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class StartupVersionLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupVersionLogger.class);

    @Bean
    ApplicationRunner logApplicationVersion(Optional<BuildProperties> buildProperties) {
        return args -> buildProperties.ifPresentOrElse(
                bp -> log.info("Application version: {}", bp.getVersion()),
                () -> log.warn("Application version: unknown (build-info not on classpath; use bootJar)")
        );
    }
}
