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

        UserRole saved = userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRoleId()).isEqualTo(roleId);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByUserIdAndRoleId_thenReturnsMatchingUserRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));

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
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(userId, anotherRoleId, Instant.now()));
        userRoleRepository.save(new UserRole(otherUserId, UUID.randomUUID(), Instant.now()));

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
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(anotherUserId, roleId, Instant.now()));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), UUID.randomUUID(), Instant.now()));

        List<UserRole> found = userRoleRepository.findByRoleId(roleId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getUserId())
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void whenExistsByRoleId_thenReturnsTrueOnlyForExistingRole() {
        UUID roleId = UUID.randomUUID();
        userRoleRepository.save(new UserRole(UUID.randomUUID(), roleId, Instant.now()));

        assertThat(userRoleRepository.existsByRoleId(roleId)).isTrue();
        assertThat(userRoleRepository.existsByRoleId(UUID.randomUUID())).isFalse();
    }
}
