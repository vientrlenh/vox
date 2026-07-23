package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.model.exam.ExamRecording;

public final class ExamRecordingDtoMapper {
    
    public static ExamRecordingDto toDto(ExamRecording recording) {
        return new ExamRecordingDto(
            recording.getId(), 
            recording.getExamSessionId(), 
            recording.getCandidateId(), 
            recording.getStreamType().name(), 
            recording.getStatus().name(), 
            recording.getSizeBytes(), 
            recording.getDurationSeconds(), 
            recording.getCreatedAt().toString(), 
            recording.getAssembledAt().toString()
        );
    }
}
