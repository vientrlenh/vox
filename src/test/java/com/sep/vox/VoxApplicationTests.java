package com.sep.vox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class VoxApplicationTests {

	@Test
	void contextLoads() {
	}

}
