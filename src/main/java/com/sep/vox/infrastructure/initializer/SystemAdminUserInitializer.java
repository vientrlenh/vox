package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.model.user.Role;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@Component
@Order(value = 2)
public class SystemAdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    public SystemAdminUserInitializer(UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository, PasswordEncoderPort passwordEncoderPort
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Value("${system-admin.email}")
    private String email;

    @Value("${system-admin.password}")
    private String password;

    @Value("${system-admin.phone}")
    private String phone;

    @Value("${system-admin.fullName}")
    private String fullName;

    @Value("${system-admin.dateOfBirth}")
    private String dateOfBirth;

    @Value("${system-admin.address}")
    private String address;

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAdminUserInitializer.class);

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Role systemAdminRole = roleRepository.findByCode("SYSTEM_ADMIN").orElse(null);
        if (systemAdminRole == null) {
            LOGGER.info("System admin role is not in database. Skip initialize system admin user");
            return;
        }
        if (userRoleRepository.existsByRoleId(systemAdminRole.getId())) {
            LOGGER.info("System admin user is already in database. Skip initialize admin user");
            return;
        }
        User systemAdminUser = systemAdminUser();
        User savedUser = userRepository.save(systemAdminUser);

        UserRole userRole = UserRole.builder()
            .userId(savedUser.getId())
            .roleId(systemAdminRole.getId())
            .createdAt(Instant.now())
            .build();
        userRoleRepository.save(userRole);
        LOGGER.info("System admin initialized successfully");
    }

    private User systemAdminUser() {
        return User.builder()
            .email(new Email(email))
            .passwordHash(passwordEncoderPort.hash(password))
            .phone(new Phone(phone))
            .fullName(new FullName(fullName))
            .gender(Gender.MALE)
            .dateOfBirth(new DateOfBirth(DateMapper.toLocalDate(dateOfBirth)))
            .address(address)
            .status(UserStatus.ACTIVE)
            .build();
    }
    
}
