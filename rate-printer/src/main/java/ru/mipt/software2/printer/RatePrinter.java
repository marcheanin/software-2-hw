package ru.mipt.software2.printer;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mipt.software2.api.currency.CurrencyServiceGrpc;
import ru.mipt.software2.api.currency.GetRateRequest;

@Component
public class RatePrinter {

    private static final Logger log = LoggerFactory.getLogger(RatePrinter.class);

    private final ServiceDiscoveryClient discoveryClient;

    public RatePrinter(ServiceDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        ServiceDiscoveryClient.InstanceInfo instance = discoveryClient.getNextInstance();
        if (instance == null) {
            log.warn("No currency-service instances available, skipping...");
            return;
        }

        try {
            ManagedChannel channel = discoveryClient.getChannel(instance);
            var stub = CurrencyServiceGrpc.newBlockingStub(channel);
            var request = GetRateRequest.getDefaultInstance();
            log.info("gRPC client request payload: {}", request);
            var response = stub.getRate(request);
            log.info("gRPC client response payload: {}", response);
            log.info("[Instance {}] USDRUB rate: {} (total instances: {})",
                    instance, response.getUsdrub(), discoveryClient.getInstanceCount());
        } catch (Exception e) {
            log.error("Failed to get rate from {}: {}", instance, e.getMessage());
        }
    }
}
