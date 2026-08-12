package com.sep.vox.interfaces.grpc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import com.sep.vox.grpc.proto.GreeterGrpc;
import com.sep.vox.grpc.proto.HealthServiceGrpc;
import com.sep.vox.interfaces.grpc.security.StreamingServiceTokenInterceptor;

@Configuration  
public class GrpcClientConfig {
    
    @Bean
    GreeterGrpc.GreeterBlockingStub greeterStub(GrpcChannelFactory channels) {
        return GreeterGrpc.newBlockingStub(channels.createChannel("greeter"));
    }

    @Bean
    HealthServiceGrpc.HealthServiceBlockingStub voxStreamingHealthStub(
            GrpcChannelFactory channels,
            @Value("${app.grpc.streaming-token:}") String streamingToken) {
        return HealthServiceGrpc.newBlockingStub(channels.createChannel("vox-streaming"))
            .withInterceptors(new StreamingServiceTokenInterceptor(streamingToken));
    }
}
