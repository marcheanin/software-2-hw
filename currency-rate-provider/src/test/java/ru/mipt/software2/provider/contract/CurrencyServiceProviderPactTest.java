package ru.mipt.software2.provider.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junit5.PluginTestTarget;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.mipt.software2.provider.CurrencyServiceImpl;
import ru.mipt.software2.provider.RateService;

import java.io.IOException;
import java.util.Map;

@Provider("currency-service")
@PactBroker(
        url = "${PACT_BROKER_URL:http://localhost:9292}"
)
@ExtendWith(PactVerificationInvocationContextProvider.class)
public class CurrencyServiceProviderPactTest {

    private static final int GRPC_PORT = 9091;
    private static Server server;

    @BeforeAll
    static void startServer() throws IOException {
        server = NettyServerBuilder.forPort(GRPC_PORT)
                .addService(new CurrencyServiceImpl(new RateService()))
                .build()
                .start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new PluginTestTarget(
                Map.of(
                        "host", "127.0.0.1",
                        "port", GRPC_PORT,
                        "transport", "grpc"
                )
        ));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
}

