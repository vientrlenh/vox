package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.infrastructure.persistence.adapter.UserRoleRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    UserRoleRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRoleRepositoryTests extends ContainerTestConfig {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void whenSave_thenReturnsPersistedUserRole() {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();

        var saved = userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRoleId()).isEqualTo(roleId);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByUserIdAndRoleId_thenReturnsMatchingUserRole() {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));

        var found = userRoleRepository.findByUserIdAndRoleId(userId, roleId);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getRoleId()).isEqualTo(roleId);
    }

    @Test
    void whenFindByUserId_thenReturnsOnlyRolesForThatUser() {
        var userId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var anotherRoleId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(userId, anotherRoleId, Instant.now()));
        userRoleRepository.save(new UserRole(otherUserId, UUID.randomUUID(), Instant.now()));

        var found = userRoleRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getRoleId())
            .containsExactlyInAnyOrder(roleId, anotherRoleId);
    }

    @Test
    void whenFindByRoleId_thenReturnsOnlyUsersForThatRole() {
        var roleId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(anotherUserId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), UUID.randomUUID(), Instant.now()));

        var found = userRoleRepository.findByRoleId(roleId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getUserId())
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void whenExistsByRoleId_thenReturnsTrueOnlyForExistingRole() {
        var roleId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(UUID.randomUUID(), roleId, Instant.now()));

        assertThat(userRoleRepository.existsByRoleId(roleId)).isTrue();
        assertThat(userRoleRepository.existsByRoleId(UUID.randomUUID())).isFalse();
    }
}
