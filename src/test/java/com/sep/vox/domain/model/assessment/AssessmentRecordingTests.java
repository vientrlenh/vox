package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentRecordingTests {

    @Test
    void registeredHasNoStorageLocationSizeOrAssemblyTimeYet() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        AssessmentRequiredStreamType streamType = AssessmentRequiredStreamType.values()[0];
        AssessmentRecordingAssemblyStatus status = AssessmentRecordingAssemblyStatus.values()[0];

        AssessmentRecording recording = AssessmentRecording.registered(sessionId, candidateId, streamType, "source", status, createdAt);

        assertThat(recording.getId()).isNull();
        assertThat(recording.getAssessmentSessionId()).isEqualTo(sessionId);
        assertThat(recording.getCandidateId()).isEqualTo(candidateId);
        assertThat(recording.getStreamType()).isEqualTo(streamType);
        assertThat(recording.getBucket()).isEmpty();
        assertThat(recording.getS3Key()).isEmpty();
        assertThat(recording.getStatus()).isEqualTo(status);
        assertThat(recording.getSizeBytes()).isNull();
        assertThat(recording.getDurationSeconds()).isNull();
        assertThat(recording.getSource()).isEqualTo("source");
        assertThat(recording.getCreatedAt()).isEqualTo(createdAt);
        assertThat(recording.getAssembledAt()).isNull();
    }
}
