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

    private final CurrencyServiceGrpc.CurrencyServiceBlockingStub blockingStub;

    public RatePrinter(ManagedChannel channel) {
        this.blockingStub = CurrencyServiceGrpc.newBlockingStub(channel);
    }

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        var response = blockingStub.getRate(GetRateRequest.getDefaultInstance());
        log.info("Current USDRUB rate: {}", response.getUsdrub());
    }
}

