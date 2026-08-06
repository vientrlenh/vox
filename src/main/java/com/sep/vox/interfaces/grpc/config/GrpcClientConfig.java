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

    /**
     * Stub gọi sang vox-streaming, ĐÃ kèm bí mật dùng chung.
     *
     * <p>Bản trước là {@code newBlockingStub(channel)} trần. vox-streaming chặn ở
     * {@code apiKeyInterceptor} nên mọi lời gọi bị trả {@code UNAUTHENTICATED}, rồi
     * {@code HealthCheckGrpcService} dịch tiếp thành HTTP 503 -- kết quả là bước xác thực OTP
     * không bao giờ qua được, kèm thông báo "Streaming service hiện không hoạt động" trong khi
     * dịch vụ vẫn sống bình thường.
     *
     * <p>Gắn ở STUB chứ không phải interceptor toàn cục: toàn cục sẽ đính token này lên cả kênh
     * {@code greeter} (vox gọi chính nó), nơi không ai đòi và cũng không nên nhận.
     */
    @Bean
    HealthServiceGrpc.HealthServiceBlockingStub voxStreamingHealthStub(
            GrpcChannelFactory channels,
            @Value("${app.grpc.streaming-token:}") String streamingToken) {
        return HealthServiceGrpc.newBlockingStub(channels.createChannel("vox-streaming"))
            .withInterceptors(new StreamingServiceTokenInterceptor(streamingToken));
    }
}
