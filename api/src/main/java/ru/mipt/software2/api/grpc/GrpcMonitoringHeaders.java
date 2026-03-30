package ru.mipt.software2.api.grpc;

import io.grpc.Metadata;

public final class GrpcMonitoringHeaders {

    public static final Metadata.Key<String> CLIENT_ID =
            Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMonitoringHeaders() {
    }
}
