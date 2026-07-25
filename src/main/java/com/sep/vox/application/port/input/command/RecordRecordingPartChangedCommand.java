package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamRecordingAssemblyStatus;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;

public record RecordRecordingPartChangedCommand(
    UUID examSessionId,
    UUID candidateId,
    ExamRequiredStreamType streamType,
    ExamRecordingAssemblyStatus status,
    String objectKey,
    Long durationSecs,
    OffsetDateTime occurredAt
) {
}
