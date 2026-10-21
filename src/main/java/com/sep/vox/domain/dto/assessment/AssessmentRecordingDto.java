package com.sep.vox.domain.dto.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

public record AssessmentRecordingDto(
    UUID id,
    UUID assessmentSessionId,
    UUID candidateId,
    AssessmentRequiredStreamType streamType,
    AssessmentRecordingAssemblyStatus status,
    Long sizeBytes,
    Integer durationSeconds,
    String source,
    boolean canonical,
    Instant createdAt,
    Instant assembledAt
) {

    public static AssessmentRecordingDto toDto(AssessmentRecording assessmentRecording, boolean canonical) {
        return new AssessmentRecordingDto(
            assessmentRecording.getId(),
            assessmentRecording.getAssessmentSessionId(),
            assessmentRecording.getCandidateId(),
            assessmentRecording.getStreamType(),
            assessmentRecording.getStatus(),
            assessmentRecording.getSizeBytes(),
            assessmentRecording.getDurationSeconds(),
            assessmentRecording.getSource(),
            canonical,
            assessmentRecording.getCreatedAt(),
            assessmentRecording.getAssembledAt()
        );
    }
}
