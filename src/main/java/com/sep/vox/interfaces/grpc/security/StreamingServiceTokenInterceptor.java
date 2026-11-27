package com.sep.vox.interfaces.grpc.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class StreamingServiceTokenInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Prefix that vox-streaming strips before comparing the token ({@code strings.TrimPrefix "Bearer "}). */
    private static final String BEARER_PREFIX = "Bearer ";

    private final String token;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // An empty token does NOT attach the header: so vox-streaming answers UNAUTHENTICATED with a clear
                // reason, instead of sending a truncated "Bearer " and getting PERMISSION_DENIED -- the two errors
                // send whoever reads the log in two different directions.
                if (token != null && !token.isBlank()) {
                    headers.put(AUTHORIZATION_KEY, BEARER_PREFIX + token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
