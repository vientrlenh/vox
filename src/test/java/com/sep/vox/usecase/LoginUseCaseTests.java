package com.sep.vox.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.command.LoginCommand;
import com.sep.vox.application.port.input.auth.LoginUseCase;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;


@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
@Transactional
public class LoginUseCaseTests {
    
    @Autowired
    private LoginUseCase loginUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    @Test
    void login_should_work() {
        var saRole = new Role(
            "SCHOOL_ADMIN",
            "School admin",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        );

        var savedRole = roleRepository.save(saRole);

        var activeUser = new User(
            new Email("test@example.com"),
            passwordEncoderPort.hash("123456"),
            new Phone("0987654321"),
            "Test User",
            null,
            LocalDate.of(2000, 1, 1),
            "Ho Chi Minh City",
            UserStatus.ACTIVE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        );

        var savedUser = userRepository.save(activeUser);

        var userRole = new UserRole(savedUser.getId(), savedRole.getId(), OffsetDateTime.now());

        userRoleRepository.save(userRole);
        
        var result = loginUseCase.execute(new LoginCommand(
            "test@example.com", 
            "123456"));
        
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotNull();
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void login_should_reject() {
        var saRole = new Role(
            "SCHOOL_ADMIN",
            "School admin",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        );

        var savedRole = roleRepository.save(saRole);

        var activeUser = new User(
            new Email("test@example.com"),
            passwordEncoderPort.hash("123456"),
            new Phone("0987654321"),
            "Test User",
            null,
            LocalDate.of(2000, 1, 1),
            "Ho Chi Minh City",
            UserStatus.ACTIVE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        );

        var savedUser = userRepository.save(activeUser);

        var userRole = new UserRole(savedUser.getId(), savedRole.getId(), OffsetDateTime.now());

        userRoleRepository.save(userRole);
        
        assertThrows(
            AuthenticationException.class, 
            () -> loginUseCase.execute(new LoginCommand(
            "test@example.com", 
            "12345"))
        );
        
        
    }
}
