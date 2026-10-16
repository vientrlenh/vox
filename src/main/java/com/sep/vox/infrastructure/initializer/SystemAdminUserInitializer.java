package com.sep.vox.infrastructure.initializer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.properties.SystemAdminInitializeProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(value = 2)
@RequiredArgsConstructor 
@Slf4j 
public class SystemAdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SystemAdminInitializeProperties systemAdminInitializeProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByRole(UserRole.SYSTEM_ADMIN)) {
            log.info("System admin user is already in database. Skip initialize admin user");
            return;
        }
        User systemAdminUser = createSystemAdminUser();
        userRepository.save(systemAdminUser);
        log.info("System admin initialized successfully");
    }

    private User createSystemAdminUser() {
        String passwordHash = passwordEncoderPort.hash(systemAdminInitializeProperties.password());
        return User.createSystemAdmin(
            systemAdminInitializeProperties.email(), 
            passwordHash, 
            systemAdminInitializeProperties.phone(), 
            systemAdminInitializeProperties.fullName(), 
            systemAdminInitializeProperties.birthDate(), 
            systemAdminInitializeProperties.address()
        );
    }
    
}
