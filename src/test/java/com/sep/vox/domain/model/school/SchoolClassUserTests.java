package com.sep.vox.domain.model.school;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchoolClassUserTests {

    @Test
    void joinedSetsEveryArgumentStartsActiveAndHasNotLeft() {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID assignedBy = UUID.randomUUID();
        Instant joinedAt = Instant.parse("2026-09-01T00:00:00Z");

        SchoolClassUser membership = SchoolClassUser.joined(userId, classId, joinedAt, assignedBy);

        assertThat(membership.getId()).isNull();
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getSchoolClassId()).isEqualTo(classId);
        assertThat(membership.isActive()).isTrue();
        assertThat(membership.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(membership.getLeftAt()).isNull();
        assertThat(membership.getAssignedBy()).isEqualTo(assignedBy);
    }
}
