package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentItemResponseTests {

    private static final Instant SUBMITTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void answeredKeepsTheGivenIdAndValuesWithoutTerminationReason() {
        UUID id = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();

        AssessmentItemResponse response = AssessmentItemResponse.answered(
                id, sessionId, paperItemId, "audio.mp3", 30, "Hello", SUBMITTED_AT);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(response.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(response.getDurationSeconds()).isEqualTo(30);
        assertThat(response.getTranscript()).isEqualTo("Hello");
        assertThat(response.getTerminationReason()).isNull();
        assertThat(response.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
    }

    @Test
    void withoutRecordingGeneratesAnIdAndHasNoAudioTranscriptOrDuration() {
        UUID sessionId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();

        AssessmentItemResponse response = AssessmentItemResponse.withoutRecording(
                sessionId, paperItemId, "NO_RECORDING", SUBMITTED_AT);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(response.getAudioUrl()).isNull();
        assertThat(response.getTranscript()).isNull();
        assertThat(response.getDurationSeconds()).isZero();
        assertThat(response.getTerminationReason()).isEqualTo("NO_RECORDING");
        assertThat(response.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
    }
}
