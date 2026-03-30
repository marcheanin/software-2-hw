package ru.mipt.software2.provider.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ru.mipt.software2.api.grpc.GrpcMonitoringHeaders;

@Component
public class GrpcServerMonitoringInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerMonitoringInterceptor.class);

    private final MeterRegistry registry;

    public GrpcServerMonitoringInterceptor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        String clientId = headers.get(GrpcMonitoringHeaders.CLIENT_ID);
        if (clientId == null || clientId.isBlank()) {
            clientId = "unknown";
        }
        String taggedClientId = clientId;

        log.info("gRPC server incoming request: method={} clientId={}", method, taggedClientId);

        Timer.Sample sample = Timer.start(registry);
        ServerCall<ReqT, RespT> wrapped = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                Timer timer = registry.timer("grpc.server.duration", "client_id", taggedClientId);
                sample.stop(timer);
                registry.counter("grpc.server.requests", "client_id", taggedClientId).increment();
                if (!status.isOk()) {
                    registry.counter(
                            "grpc.server.errors",
                            "status", status.getCode().name(),
                            "client_id", taggedClientId
                    ).increment();
                }
                log.info("gRPC server outgoing response: method={} clientId={} status={}",
                        method, taggedClientId, status);
                super.close(status, trailers);
            }
        };
        return next.startCall(wrapped, headers);
    }
}
