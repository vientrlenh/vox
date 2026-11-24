package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentMemberTests {

    @Test
    void grantedRecordsWhoGrantedTheRoleAndWhen() {
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID grantedBy = UUID.randomUUID();
        Instant grantedAt = Instant.parse("2026-01-01T00:00:00Z");

        AssessmentMember member = AssessmentMember.granted(assessmentId, userId, AssessmentMemberRole.CHAIR, grantedAt, grantedBy);

        assertThat(member.getId()).isNull();
        assertThat(member.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getRole()).isEqualTo(AssessmentMemberRole.CHAIR);
        assertThat(member.getGrantedAt()).isEqualTo(grantedAt);
        assertThat(member.getGrantedBy()).isEqualTo(grantedBy);
    }
}
