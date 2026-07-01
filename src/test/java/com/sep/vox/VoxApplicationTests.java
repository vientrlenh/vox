package com.sep.vox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
class VoxApplicationTests extends ContainerTestConfig {

	@Test
	void contextLoads() {
	}

}
