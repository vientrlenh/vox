package com.sep.vox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;

import com.sep.vox.support.IntegrationTest;

@IntegrationTest
@AutoConfigureTestGrpcTransport
class VoxApplicationTests {

	@Test
	void contextLoads() {
	}

}
