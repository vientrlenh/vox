package com.sep.vox.domain.model.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRoleTests {

    @Test
    void assignedSetsEveryArgument() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-01T00:00:00Z");

        UserRole userRole = UserRole.assigned(userId, roleId, createdAt);

        assertThat(userRole.getId()).isNull();
        assertThat(userRole.getUserId()).isEqualTo(userId);
        assertThat(userRole.getRoleId()).isEqualTo(roleId);
        assertThat(userRole.getCreatedAt()).isEqualTo(createdAt);
    }
}
