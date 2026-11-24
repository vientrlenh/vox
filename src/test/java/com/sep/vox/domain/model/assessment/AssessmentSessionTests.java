package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentSessionTests {

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final Instant at = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void startedIsInProgressAndNotSubmittedOrFlagged() {
        AssessmentSession session = AssessmentSession.started(assessmentId, candidateId, paperId, at);

        assertThat(session.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(session.getCandidateId()).isEqualTo(candidateId);
        assertThat(session.getPaperId()).isEqualTo(paperId);
        assertThat(session.getStartedAt()).isEqualTo(at);
        assertThat(session.getSubmittedAt()).isNull();
        assertThat(session.getStatus()).isEqualTo(AssessmentSessionStatus.IN_PROGRESS);
        assertThat(session.isFlagged()).isFalse();
        assertThat(session.getFlagReason()).isNull();
    }

    @Test
    void gradedWithoutAnswersStartsAndSubmitsAtTheSameInstant() {
        AssessmentSession session = AssessmentSession.gradedWithoutAnswers(assessmentId, candidateId, paperId, at);

        assertThat(session.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(session.getCandidateId()).isEqualTo(candidateId);
        assertThat(session.getPaperId()).isEqualTo(paperId);
        assertThat(session.getStartedAt()).isEqualTo(at);
        assertThat(session.getSubmittedAt()).isEqualTo(at);
        assertThat(session.getStatus()).isEqualTo(AssessmentSessionStatus.GRADED);
        assertThat(session.isFlagged()).isFalse();
        assertThat(session.getFlagReason()).isNull();
    }
}
