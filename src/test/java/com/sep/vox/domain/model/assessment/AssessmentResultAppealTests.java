package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentResultAppealTests {

    @Test
    void requestedIsPendingWithNoDecisionYet() {
        UUID candidateResultId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-01-01T00:00:00Z");

        AssessmentResultAppeal appeal = AssessmentResultAppeal.requested(
            candidateResultId, requestedBy, "reason", requestedAt, new BigDecimal("6.5"), "notes");

        assertThat(appeal.getId()).isNull();
        assertThat(appeal.getCandidateResultId()).isEqualTo(candidateResultId);
        assertThat(appeal.getRequestedBy()).isEqualTo(requestedBy);
        assertThat(appeal.getReason()).isEqualTo("reason");
        assertThat(appeal.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.PENDING);
        assertThat(appeal.getScoreBefore()).isEqualByComparingTo("6.5");
        assertThat(appeal.getNotes()).isEqualTo("notes");
        assertThat(appeal.getScoreAfter()).isNull();
        assertThat(appeal.getResolvedBy()).isNull();
        assertThat(appeal.getResolvedAt()).isNull();
        assertThat(appeal.getDeadline()).isNull();
        assertThat(appeal.getApprovedAt()).isNull();
        assertThat(appeal.getDecisionNote()).isNull();
        assertThat(appeal.getWithdrawnAt()).isNull();
        assertThat(appeal.getReviewerOverrideReason()).isNull();
    }
}
