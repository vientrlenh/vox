package com.sep.vox.interfaces.grpc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import com.sep.vox.grpc.proto.GreeterGrpc;
import com.sep.vox.grpc.proto.HealthServiceGrpc;

@Configuration  
public class GrpcClientConfig {
    
    @Bean
    GreeterGrpc.GreeterBlockingStub greeterStub(GrpcChannelFactory channels) {
        return GreeterGrpc.newBlockingStub(channels.createChannel("greeter"));
    }

    @Bean
    HealthServiceGrpc.HealthServiceBlockingStub voxStreamingHealthStub(GrpcChannelFactory channels) {
        return HealthServiceGrpc.newBlockingStub(channels.createChannel("vox-streaming"));
    }
}
