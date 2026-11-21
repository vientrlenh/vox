package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.adapter.UserRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    UserRepositoryImpl.class
})
public class UserRepositoryTests {
    
    @Autowired
    private UserRepository userRepository;

    @Test
    void whenSave_thenReturnsPersistedUser() {
        User user = newUser("save-user@example.com", "0987654321");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail().value()).isEqualTo("save-user@example.com");
        assertThat(saved.getPhone().value()).isEqualTo("0987654321");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void whenFindById_thenReturnsUser() {
        User saved = userRepository.save(newUser("find-id@example.com", "0987654322"));

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail().value()).isEqualTo("find-id@example.com");
    }

    @Test
    void whenFindByIdIn_thenReturnsMatchingUsers() {
        User saved1 = userRepository.save(newUser("find-id-in-1@example.com", "0987654331"));
        User saved2 = userRepository.save(newUser("find-id-in-2@example.com", "0987654332"));
        userRepository.save(newUser("find-id-in-other@example.com", "0987654333"));

        List<User> found = userRepository.findByIdIn(java.util.Set.of(saved1.getId(), saved2.getId()));

        assertThat(found)
            .extracting(user -> user.getEmail().value())
            .containsExactlyInAnyOrder("find-id-in-1@example.com", "find-id-in-2@example.com");
    }

    @Test
    void whenFindByEmailIn_thenReturnsMatchingUsers() {
        userRepository.save(newUser("find-email-in-1@example.com", "0987654341"));
        userRepository.save(newUser("find-email-in-2@example.com", "0987654342"));
        userRepository.save(newUser("find-email-in-other@example.com", "0987654343"));

        List<User> found = userRepository.findByEmailIn(Set.of("find-email-in-1@example.com", "find-email-in-2@example.com", "missing@example.com"));

        assertThat(found)
            .extracting(user -> user.getEmail().value())
            .containsExactlyInAnyOrder("find-email-in-1@example.com", "find-email-in-2@example.com");
    }

    @Test
    void whenFindByEmail_thenReturnsUser() {
        userRepository.save(newUser("test@example.com", "0987654323"));

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail().value()).isEqualTo("test@example.com");
        assertThat(found.get().getPhone().value()).isEqualTo("0987654323");
    }

    @Test
    void whenFindByEmailAndStatus_thenReturnsOnlyMatchingStatus() {
        userRepository.save(newUser("active@example.com", "0987654325", UserStatus.ACTIVE));
        userRepository.save(newUser("inactive@example.com", "0987654326", UserStatus.INACTIVE));

        Optional<User> activeUser = userRepository.findByEmailAndStatus("active@example.com", UserStatus.ACTIVE);
        Optional<User> inactiveAsActive = userRepository.findByEmailAndStatus("inactive@example.com", UserStatus.ACTIVE);

        assertThat(activeUser).isPresent();
        assertThat(activeUser.get().getEmail().value()).isEqualTo("active@example.com");
        assertThat(activeUser.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(inactiveAsActive).isEmpty();
    }

    @Test
    void whenFindByPhone_thenReturnsUser() {
        userRepository.save(newUser("find-phone@example.com", "0987654324"));

        Optional<User> found = userRepository.findByPhone("0987654324");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail().value()).isEqualTo("find-phone@example.com");
        assertThat(found.get().getPhone().value()).isEqualTo("0987654324");
    }

    private static User newUser(String email, String phone) {
        return newUser(email, phone, UserStatus.ACTIVE);
    }

    private static User newUser(String email, String phone, UserStatus status) {
        Instant now = Instant.now();
        return User.builder()
            .email(new Email(email))
            .passwordHash("password-hash")
            .phone(new Phone(phone))
            .fullName(new FullName("Test User"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2000, 1, 1)))
            .address("Ho Chi Minh City")
            .status(status)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
