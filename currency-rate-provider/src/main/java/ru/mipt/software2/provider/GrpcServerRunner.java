package ru.mipt.software2.provider;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;

@Component
public class GrpcServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);
    private static final int GRPC_PORT = 9090;

    private final CurrencyServiceImpl currencyService;
    private Server server;

    public GrpcServerRunner(CurrencyServiceImpl currencyService) {
        this.currencyService = currencyService;
    }

    @Override
    public void run(String... args) throws Exception {
        start();
        // Блокируем основной поток, чтобы приложение не завершилось сразу
        server.awaitTermination();
    }

    private void start() throws IOException {
        server = NettyServerBuilder.forPort(GRPC_PORT)
                .addService(currencyService)
                .build()
                .start();

        log.info("gRPC server started on port {}", GRPC_PORT);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("gRPC server stopped");
        }
    }
}

