package ru.mipt.software2.provider;

import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

@Component
public class GrpcServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    @Value("${grpc.server.host:}")
    private String grpcHost;

    @Value("${service.name:currency-service}")
    private String serviceName;

    private final CurrencyServiceImpl currencyService;
    private final CuratorFramework curatorFramework;
    private Server server;
    private String registeredPath;

    public GrpcServerRunner(CurrencyServiceImpl currencyService, CuratorFramework curatorFramework) {
        this.currencyService = currencyService;
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void run(String... args) throws Exception {
        startGrpcServer();
        registerInZookeeper();
        server.awaitTermination();
    }

    private void startGrpcServer() throws IOException {
        server = NettyServerBuilder.forPort(grpcPort)
                .addService(currencyService)
                .build()
                .start();
        log.info("gRPC server started on port {}", grpcPort);
    }

    private void registerInZookeeper() throws Exception {
        String host = resolveHost();
        String instanceData = host + ":" + grpcPort;
        String basePath = "/services/" + serviceName + "/instance-";

        registeredPath = curatorFramework.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(basePath, instanceData.getBytes(StandardCharsets.UTF_8));

        log.info("Registered in ZooKeeper: {} -> {}", registeredPath, instanceData);
    }

    private String resolveHost() {
        if (grpcHost != null && !grpcHost.isEmpty()) {
            return grpcHost;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Could not resolve host address, falling back to localhost");
            return "localhost";
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("gRPC server stopped");
        }
    }
}
