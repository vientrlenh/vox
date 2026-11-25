package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticePaperTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:10:00Z");

    @Test
    void reservedSetsStatusAndReservesTheSpokenSeconds() {
        UUID studentId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID bandId = UUID.randomUUID();

        PracticePaper paper = PracticePaper.reserved(
                studentId, topicId, bandId, "EPSILON", "GOAL", "[\"a\"]", "[]", 90, EXPIRES_AT, CREATED_AT);

        assertThat(paper.getId()).isNull();
        assertThat(paper.getStudentId()).isEqualTo(studentId);
        assertThat(paper.getPracticeTopicId()).isEqualTo(topicId);
        assertThat(paper.getTargetFrameworkBandId()).isEqualTo(bandId);
        assertThat(paper.getOrigin()).isEqualTo("EPSILON");
        assertThat(paper.getGoalAtBuild()).isEqualTo("GOAL");
        assertThat(paper.getOfferedTopicIdsJson()).isEqualTo("[\"a\"]");
        assertThat(paper.getPreviousOfferedTopicIdsJson()).isEqualTo("[]");
        assertThat(paper.getPlannedSeconds()).isEqualTo(90);
        assertThat(paper.getReservedQuotaSeconds()).isEqualTo(90);
        assertThat(paper.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(paper.getStatus()).isEqualTo("RESERVED");
        assertThat(paper.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void reservedDefaultsAMissingOriginToSelected() {
        PracticePaper paper = PracticePaper.reserved(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, "GOAL", "[]", "[]", 60,
                EXPIRES_AT, CREATED_AT);

        assertThat(paper.getOrigin()).isEqualTo("SELECTED");
    }
}
