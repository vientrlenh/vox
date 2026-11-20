package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.model.exam.ExamRecording;

public final class ExamRecordingDtoMapper {
    
    public static ExamRecordingDto toDto(ExamRecording recording, boolean canonical) {
        return new ExamRecordingDto(
            recording.getId(),
            recording.getExamSessionId(),
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
