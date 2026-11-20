package com.sep.vox.application.query.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

class ProctorCandidateSummaryTests {

    private static ProctorCandidateSummary fromColumns(String status, String sessionStatus) {
        return new ProctorCandidateSummary(UUID.randomUUID(), UUID.randomUUID(), "An", "an@example.com", status, null,
            UUID.randomUUID(), sessionStatus, false);
    }

    @Test
    void should_convert_the_status_columns_into_enums() {
        ProctorCandidateSummary summary = fromColumns("ATTENDED", "GRADING");

        assertThat(summary.status()).isEqualTo(AssessmentCandidateStatus.ATTENDED);
        assertThat(summary.sessionStatus()).isEqualTo(AssessmentSessionStatus.GRADING);
    }

    @Test
    void should_keep_the_session_status_null_when_the_candidate_has_no_session() {
        ProctorCandidateSummary summary = fromColumns("ASSIGNED", null);

        assertThat(summary.sessionStatus()).isNull();
    }

    @Test
    void should_reject_a_status_column_that_is_not_an_enum_name() {
        assertThatThrownBy(() -> fromColumns("NOT_A_STATUS", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
