package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.infrastructure.persistence.adapter.UserRoleRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    UserRoleRepositoryImpl.class
})
class UserRoleRepositoryTests {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void whenSave_thenReturnsPersistedUserRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserRole saved = userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRoleId()).isEqualTo(roleId);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByUserIdAndRoleId_thenReturnsMatchingUserRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());

        Optional<UserRole> found = userRoleRepository.findByUserIdAndRoleId(userId, roleId);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getRoleId()).isEqualTo(roleId);
    }

    @Test
    void whenFindByUserId_thenReturnsOnlyRolesForThatUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID anotherRoleId = UUID.randomUUID();
        userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());
        userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(anotherRoleId)
            .createdAt(Instant.now())
            .build());
        userRoleRepository.save(UserRole.builder()
            .userId(otherUserId)
            .roleId(UUID.randomUUID())
            .createdAt(Instant.now())
            .build());

        List<UserRole> found = userRoleRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getRoleId())
            .containsExactlyInAnyOrder(roleId, anotherRoleId);
    }

    @Test
    void whenFindByRoleId_thenReturnsOnlyUsersForThatRole() {
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());
        userRoleRepository.save(UserRole.builder()
            .userId(anotherUserId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());
        userRoleRepository.save(UserRole.builder()
            .userId(UUID.randomUUID())
            .roleId(UUID.randomUUID())
            .createdAt(Instant.now())
            .build());

        List<UserRole> found = userRoleRepository.findByRoleId(roleId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getUserId())
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void whenExistsByRoleId_thenReturnsTrueOnlyForExistingRole() {
        UUID roleId = UUID.randomUUID();
        userRoleRepository.save(UserRole.builder()
            .userId(UUID.randomUUID())
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());

        assertThat(userRoleRepository.existsByRoleId(roleId)).isTrue();
        assertThat(userRoleRepository.existsByRoleId(UUID.randomUUID())).isFalse();
    }
}
