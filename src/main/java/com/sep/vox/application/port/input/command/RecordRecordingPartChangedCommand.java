package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

public record RecordRecordingPartChangedCommand(
    UUID assessmentSessionId,
    UUID candidateId,
    AssessmentRequiredStreamType streamType,
    AssessmentRecordingAssemblyStatus status,
    String objectKey,
    Long durationSecs,
    Instant occurredAt,
    String source
) {
}
