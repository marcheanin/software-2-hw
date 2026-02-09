package ru.mipt.software2.printer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    private static final String TARGET = "localhost:9090";

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder
                .forTarget(TARGET)
                .usePlaintext()
                .build();
    }
}

