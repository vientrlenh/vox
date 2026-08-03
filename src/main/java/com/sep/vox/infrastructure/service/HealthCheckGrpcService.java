package com.sep.vox.infrastructure.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.port.output.HealthCheckPort;
import com.sep.vox.grpc.proto.HealthRequest;
import com.sep.vox.grpc.proto.HealthServiceGrpc;

import io.grpc.StatusRuntimeException;

@Component
public class HealthCheckGrpcService implements HealthCheckPort {
    
    private final HealthServiceGrpc.HealthServiceBlockingStub streamingHealthServiceBlockingStub;

    public HealthCheckGrpcService(HealthServiceGrpc.HealthServiceBlockingStub streamingHealthServiceBlockingStub) {
        this.streamingHealthServiceBlockingStub = streamingHealthServiceBlockingStub;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckGrpcService.class);

    @Override
    public void checkStreamingOk() {
        try {
            var response = streamingHealthServiceBlockingStub
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .ping(HealthRequest.getDefaultInstance());
            if (!response.getAlive()) {
                throw new ServiceUnavailableException(response.getMessage());
            }
        } catch (StatusRuntimeException e) {
            LOGGER.error("Streaming service status: {}", e.getStatus().toString());
            throw new ServiceUnavailableException("Streaming service hiện không hoạt động");
        }
    }

    
}
