package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentResultStatusHistoryTests {

    @Test
    void transitionSetsTheGivenValuesWithoutId() {
        UUID resultId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        AssessmentResultStatusHistory history = AssessmentResultStatusHistory.transition(
                resultId,
                AssessmentCandidateResultStatus.PENDING_REVIEW,
                AssessmentCandidateResultStatus.RELEASED,
                new BigDecimal("6"),
                new BigDecimal("7"),
                ResultStatusChangeSource.ADMIN_BULK_FINALIZE,
                actorId,
                "Bulk release",
                createdAt);

        assertThat(history.getCandidateResultId()).isEqualTo(resultId);
        assertThat(history.getFromStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
        assertThat(history.getToStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(history.getScoreBefore()).isEqualByComparingTo("6");
        assertThat(history.getScoreAfter()).isEqualByComparingTo("7");
        assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.ADMIN_BULK_FINALIZE);
        assertThat(history.getActorId()).isEqualTo(actorId);
        assertThat(history.getReason()).isEqualTo("Bulk release");
        assertThat(history.getCreatedAt()).isEqualTo(createdAt);
        assertThat(history.getId()).isNull();
    }
}
