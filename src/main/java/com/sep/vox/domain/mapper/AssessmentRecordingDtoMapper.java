package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.AssessmentRecordingDto;
import com.sep.vox.domain.model.assessment.AssessmentRecording;

public final class AssessmentRecordingDtoMapper {
    
    public static AssessmentRecordingDto toDto(AssessmentRecording recording, boolean canonical) {
        return new AssessmentRecordingDto(
            recording.getId(),
            recording.getAssessmentSessionId(),
            recording.getCandidateId(),
            recording.getStreamType().name(),
            recording.getStatus().name(),
            recording.getSizeBytes(),
            recording.getDurationSeconds(),
            recording.getSource(),
            canonical,
            recording.getCreatedAt().toString(),
            recording.getAssembledAt() == null ? null : recording.getAssembledAt().toString()
        );
    }
}
