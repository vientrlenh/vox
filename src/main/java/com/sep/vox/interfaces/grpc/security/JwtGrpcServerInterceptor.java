package com.sep.vox.interfaces.grpc.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthTokenPort;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;


@Component
@GlobalServerInterceptor
public class JwtGrpcServerInterceptor implements ServerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtGrpcServerInterceptor.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final AuthTokenPort authTokenPort;
    private final UserDetailsService userDetailsService;

    public JwtGrpcServerInterceptor(AuthTokenPort authTokenPort, UserDetailsService userDetailsService) {
        this.authTokenPort = authTokenPort;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        UsernamePasswordAuthenticationToken authentication = authenticate(headers);


        SecurityContext context = SecurityContextHolder.createEmptyContext();
        if (authentication != null) {
            context.setAuthentication(authentication);
        }

        ServerCall<ReqT, RespT> wrappedCall = new SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                runWithContext(context, () -> super.close(status, trailers));
            }
        };

        Listener<ReqT> delegate = next.startCall(wrappedCall, headers);
        return new SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                runWithContext(context, () -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                runWithContext(context, super::onHalfClose);
            }

            @Override
            public void onReady() {
                runWithContext(context, super::onReady);
            }

            @Override
            public void onCancel() {
                runWithContext(context, super::onCancel);
            }

            @Override
            public void onComplete() {
                runWithContext(context, super::onComplete);
            }
        };
    }

    private UsernamePasswordAuthenticationToken authenticate(Metadata headers) {
        String header = headers.get(AUTHORIZATION_KEY);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null; 
        }
        try {
            String token = header.substring(BEARER_PREFIX.length());
            String email = authTokenPort.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!userDetails.isEnabled()) {
                LOGGER.warn("gRPC auth rejected: disabled user {}", email);
                return null;
            }
            return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
        } catch (Exception e) {
            LOGGER.warn("gRPC JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    private void runWithContext(SecurityContext context, Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
