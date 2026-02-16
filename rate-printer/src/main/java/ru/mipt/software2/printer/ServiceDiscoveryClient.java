package ru.mipt.software2.printer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ServiceDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryClient.class);

    private final CuratorFramework curator;
    private final String basePath;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentMap<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    public ServiceDiscoveryClient(CuratorFramework curator,
                                   @Value("${service.name:currency-service}") String serviceName) {
        this.curator = curator;
        this.basePath = "/services/" + serviceName;
    }

    public InstanceInfo getNextInstance() {
        try {
            if (curator.checkExists().forPath(basePath) == null) {
                return null;
            }

            List<String> children = curator.getChildren().forPath(basePath);
            if (children.isEmpty()) {
                return null;
            }

            List<InstanceInfo> instances = new ArrayList<>();
            for (String child : children) {
                byte[] data = curator.getData().forPath(basePath + "/" + child);
                String address = new String(data, StandardCharsets.UTF_8);
                String[] parts = address.split(":");
                if (parts.length == 2) {
                    instances.add(new InstanceInfo(parts[0], Integer.parseInt(parts[1])));
                }
            }

            if (instances.isEmpty()) {
                return null;
            }

            int idx = Math.abs(counter.getAndIncrement() % instances.size());
            return instances.get(idx);
        } catch (Exception e) {
            log.error("Failed to discover service instances", e);
            return null;
        }
    }

    public ManagedChannel getChannel(InstanceInfo instance) {
        String key = instance.host() + ":" + instance.port();
        return channelCache.computeIfAbsent(key, k ->
                ManagedChannelBuilder.forAddress(instance.host(), instance.port())
                        .usePlaintext()
                        .build()
        );
    }

    public int getInstanceCount() {
        try {
            if (curator.checkExists().forPath(basePath) == null) {
                return 0;
            }
            return curator.getChildren().forPath(basePath).size();
        } catch (Exception e) {
            return 0;
        }
    }

    @PreDestroy
    public void close() {
        channelCache.values().forEach(ManagedChannel::shutdown);
    }

    public record InstanceInfo(String host, int port) {
        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
