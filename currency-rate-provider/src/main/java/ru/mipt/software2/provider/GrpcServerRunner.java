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
import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyServerBuilder;
import ru.mipt.software2.provider.grpc.GrpcServerMonitoringInterceptor;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GrpcServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    @Value("${grpc.server.host:}")
    private String grpcHost;

    @Value("${service.name:currency-service}")
    private String serviceName;

    @Value("${app.grpc.server.shutdown-grace-seconds:20}")
    private int shutdownGraceSeconds;

    @Value("${app.grpc.server.shutdown-force-seconds:5}")
    private int shutdownForceSeconds;

    private final CurrencyServiceImpl currencyService;
    private final CuratorFramework curatorFramework;
    private final GrpcServerMonitoringInterceptor grpcServerMonitoringInterceptor;
    private Server server;
    private String registeredPath;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public GrpcServerRunner(CurrencyServiceImpl currencyService,
                            CuratorFramework curatorFramework,
                            GrpcServerMonitoringInterceptor grpcServerMonitoringInterceptor) {
        this.currencyService = currencyService;
        this.curatorFramework = curatorFramework;
        this.grpcServerMonitoringInterceptor = grpcServerMonitoringInterceptor;
    }

    @Override
    public void run(String... args) throws Exception {
        startGrpcServer();
        registerInZookeeper();
        server.awaitTermination();
    }

    private void startGrpcServer() throws IOException {
        server = NettyServerBuilder.forPort(grpcPort)
                .addService(ServerInterceptors.intercept(currencyService, grpcServerMonitoringInterceptor))
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
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        if (server == null) {
            return;
        }
        if (server.isShutdown()) {
            return;
        }
        log.info("Stopping gRPC server (grace {}s, force {}s)", shutdownGraceSeconds, shutdownForceSeconds);
        server.shutdown();
        try {
            if (!server.awaitTermination(shutdownGraceSeconds, TimeUnit.SECONDS)) {
                log.warn("gRPC server did not finish within {}s; forcing shutdownNow()", shutdownGraceSeconds);
                server.shutdownNow();
                if (!server.awaitTermination(shutdownForceSeconds, TimeUnit.SECONDS)) {
                    log.error("gRPC server still not terminated after shutdownNow()");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        } finally {
            removeZookeeperRegistration();
            log.info("gRPC server stopped");
        }
    }

    private void removeZookeeperRegistration() {
        if (registeredPath == null) {
            return;
        }
        try {
            curatorFramework.delete().deletingChildrenIfNeeded().forPath(registeredPath);
            log.info("Removed ZooKeeper registration {}", registeredPath);
        } catch (Exception e) {
            log.warn("Could not delete ZooKeeper path {}: {}", registeredPath, e.getMessage());
        } finally {
            registeredPath = null;
        }
    }
}
