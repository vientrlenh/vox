package com.sep.vox.interfaces.grpc.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;


public class StreamingServiceTokenInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Tiền tố vox-streaming cắt bỏ trước khi so token ({@code strings.TrimPrefix "Bearer "}). */
    private static final String BEARER_PREFIX = "Bearer ";

    private final String token;

    public StreamingServiceTokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Token rỗng thì KHÔNG gắn header: để vox-streaming trả UNAUTHENTICATED với lý
                // do rõ ràng, thay vì gửi "Bearer " cụt và nhận PERMISSION_DENIED -- hai lỗi đó
                // dẫn người đọc log đi hai hướng khác nhau.
                if (token != null && !token.isBlank()) {
                    headers.put(AUTHORIZATION_KEY, BEARER_PREFIX + token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
