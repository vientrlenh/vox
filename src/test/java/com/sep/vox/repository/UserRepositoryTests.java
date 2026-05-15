package com.sep.vox.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;

import jakarta.transaction.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
public class UserRepositoryTests {
    
    @Autowired
    private UserRepository userRepository;


    @Test
    void whenFindByEmail_thenReturnsUser() {
        
        var user = new User(
            new Email("test@example.com"),
            "password-hash",
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

        userRepository.save(user);

        var found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail().value()).isEqualTo("test@example.com");
        assertThat(found.get().getPhone().value()).isEqualTo("0987654321");
    }
}
