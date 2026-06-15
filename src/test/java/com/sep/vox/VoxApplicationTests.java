package com.sep.vox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;


@SpringBootTest
@ActiveProfiles("test")
class VoxApplicationTests extends ContainerTestConfig {

	@Test
	void contextLoads() {
	}

}
