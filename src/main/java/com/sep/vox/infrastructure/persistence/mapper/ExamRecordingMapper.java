package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.model.exam.ExamRecordingAssemblyStatus;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.infrastructure.persistence.entity.ExamRecordingJpaEntity;

public final class ExamRecordingMapper {
    
    public static ExamRecording toDomain(ExamRecordingJpaEntity jpa) {
        return new ExamRecording(
            jpa.getId(), 
            jpa.getExamSessionId(), 
            jpa.getCandidateId(), 
            streamTypeFromString(jpa.getStreamType()), 
            jpa.getBucket(), 
            jpa.getS3Key(), 
            statusFromString(jpa.getStatus()), 
            jpa.getSizeBytes(), 
            jpa.getDurationSeconds(), 
            jpa.getSource(),
            jpa.getCreatedAt(), 
            jpa.getAssembledAt()
        );
    }

    public static ExamRecordingJpaEntity toJpa(ExamRecording recording) {
        return new ExamRecordingJpaEntity(
            recording.getId(), 
            recording.getExamSessionId(), 
            recording.getCandidateId(), 
            valueOf(recording.getStreamType()), 
            recording.getBucket(), 
            recording.getS3Key(), 
            valueOf(recording.getStatus()), 
            recording.getSizeBytes(), 
            recording.getDurationSeconds(), 
            recording.getSource(),
            recording.getCreatedAt(), 
            recording.getAssembledAt()
        );
    }


    private static ExamRequiredStreamType streamTypeFromString(String value) {
        return value == null ? null : ExamRequiredStreamType.valueOf(value);
    }

    private static ExamRecordingAssemblyStatus statusFromString(String value) {
        return value == null ? null : ExamRecordingAssemblyStatus.valueOf(value);
    }

    private static String valueOf(ExamRequiredStreamType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ExamRecordingAssemblyStatus status) {
        return status == null ? null : status.name();
    }
}
