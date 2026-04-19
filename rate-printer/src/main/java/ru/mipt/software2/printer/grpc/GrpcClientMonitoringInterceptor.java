package ru.mipt.software2.printer.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mipt.software2.api.grpc.GrpcMonitoringHeaders;

@Component
public class GrpcClientMonitoringInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientMonitoringInterceptor.class);

    private final String clientId;

    public GrpcClientMonitoringInterceptor(@Value("${spring.application.name}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        log.info("gRPC client outgoing request: method={} clientId={}", method.getFullMethodName(), clientId);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(GrpcMonitoringHeaders.CLIENT_ID, clientId);
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        log.info("gRPC client incoming response: method={} clientId={} status={}",
                                method.getFullMethodName(), clientId, status);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
