package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticeSessionTests {

    @Test
    void startedBeginsInProgressWithZeroedCounters() {
        UUID studentId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();
        UUID bandId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");

        PracticeSession session = PracticeSession.started(
                studentId, paperId, bandId, topicId, "SELECTED", "[\"t\"]", startedAt);

        assertThat(session.getId()).isNull();
        assertThat(session.getStudentId()).isEqualTo(studentId);
        assertThat(session.getPracticePaperId()).isEqualTo(paperId);
        assertThat(session.getTargetFrameworkBandId()).isEqualTo(bandId);
        assertThat(session.getChosenPracticeTopicId()).isEqualTo(topicId);
        assertThat(session.getTargetSubAttributesJson()).isEqualTo("[]");
        assertThat(session.getOrigin()).isEqualTo("SELECTED");
        assertThat(session.getOfferedTopicIdsJson()).isEqualTo("[\"t\"]");
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.getLastHeartbeatAt()).isEqualTo(startedAt);
        assertThat(session.getGradedSeconds()).isZero();
        assertThat(session.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(session.getHelpRequestCount()).isZero();
        assertThat(session.getLongPauseCount()).isZero();
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.getOverallScore()).isNull();
    }
}
