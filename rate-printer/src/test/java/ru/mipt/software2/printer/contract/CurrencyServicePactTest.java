package ru.mipt.software2.printer.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.mipt.software2.api.currency.CurrencyServiceGrpc;
import ru.mipt.software2.api.currency.GetRateRequest;
import ru.mipt.software2.api.currency.GetRateResponse;

import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.PactBuilder.filePath;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "currency-service",
        providerType = ProviderType.SYNCH_MESSAGE,
        pactVersion = PactSpecVersion.V4
)
public class CurrencyServicePactTest {

    @Pact(consumer = "rate-printer")
    V4Pact getRateInteraction(PactBuilder builder) {
        return builder
                .usingPlugin("protobuf")
                .expectsToReceive(
                        "get currency rate",
                        "core/interaction/synchronous-message"
                )
                .with(Map.of(
                        "pact:proto", filePath("../api/src/main/proto/currency.proto"),
                        "pact:content-type", "application/grpc",
                        "pact:proto-service", "CurrencyService/GetRate",
                        "request", Map.of(),
                        "response", List.of(
                                Map.of(
                                        "usdrub", "matching(number, 100.0)"
                                )
                        )
                ))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getRateInteraction")
    @MockServerConfig(
            implementation = MockServerImplementation.Plugin,
            registryEntry = "protobuf/transport/grpc"
    )
    void getRate(MockServer mockServer) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("127.0.0.1:" + mockServer.getPort())
                .usePlaintext()
                .build();

        CurrencyServiceGrpc.CurrencyServiceBlockingStub stub =
                CurrencyServiceGrpc.newBlockingStub(channel);

        GetRateResponse response = stub.getRate(GetRateRequest.getDefaultInstance());

        assertTrue(response.getUsdrub() > 0.0);
    }
}

