package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentProctoringAlertTests {

    @Test
    void raisedCopiesEveryArgumentIntoItsField() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        Instant capturedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant raisedAt = Instant.parse("2026-01-01T00:00:01Z");
        Instant createdAt = Instant.parse("2026-01-01T00:00:02Z");

        AssessmentProctoringAlert alert = AssessmentProctoringAlert.raised("event-1", sessionId, candidateId, "stream-1",
            "SCREEN", "TAB_SWITCH", "HIGH", "AI", "detail", new BigDecimal("0.9"), 7L, capturedAt, raisedAt, createdAt);

        assertThat(alert.getId()).isNull();
        assertThat(alert.getEventId()).isEqualTo("event-1");
        assertThat(alert.getAssessmentSessionId()).isEqualTo(sessionId);
        assertThat(alert.getCandidateId()).isEqualTo(candidateId);
        assertThat(alert.getStreamId()).isEqualTo("stream-1");
        assertThat(alert.getStreamType()).isEqualTo("SCREEN");
        assertThat(alert.getAlertType()).isEqualTo("TAB_SWITCH");
        assertThat(alert.getLevel()).isEqualTo("HIGH");
        assertThat(alert.getSource()).isEqualTo("AI");
        assertThat(alert.getDetail()).isEqualTo("detail");
        assertThat(alert.getConfidence()).isEqualByComparingTo("0.9");
        assertThat(alert.getSequenceNo()).isEqualTo(7L);
        assertThat(alert.getCapturedAt()).isEqualTo(capturedAt);
        assertThat(alert.getRaisedAt()).isEqualTo(raisedAt);
        assertThat(alert.getCreatedAt()).isEqualTo(createdAt);
    }
}
