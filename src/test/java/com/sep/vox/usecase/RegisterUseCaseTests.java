package com.sep.vox.usecase;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.TestContainerConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
@Transactional
public class RegisterUseCaseTests {
    
}
