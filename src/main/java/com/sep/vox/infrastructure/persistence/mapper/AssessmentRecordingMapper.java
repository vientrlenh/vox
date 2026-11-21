package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentRecordingJpaEntity;

public final class AssessmentRecordingMapper {
    
    public static AssessmentRecording toDomain(AssessmentRecordingJpaEntity jpa) {
        return AssessmentRecording.builder()
                .id(jpa.getId())
                .assessmentSessionId(jpa.getAssessmentSessionId())
                .candidateId(jpa.getCandidateId())
                .streamType(streamTypeFromString(jpa.getStreamType()))
                .bucket(jpa.getBucket())
                .s3Key(jpa.getS3Key())
                .status(statusFromString(jpa.getStatus()))
                .sizeBytes(jpa.getSizeBytes())
                .durationSeconds(jpa.getDurationSeconds())
                .source(jpa.getSource())
                .createdAt(jpa.getCreatedAt())
                .assembledAt(jpa.getAssembledAt())
                .build();
    }

    public static AssessmentRecordingJpaEntity toJpa(AssessmentRecording recording) {
        return AssessmentRecordingJpaEntity.builder()
                .id(recording.getId())
                .assessmentSessionId(recording.getAssessmentSessionId())
                .candidateId(recording.getCandidateId())
                .streamType(valueOf(recording.getStreamType()))
                .bucket(recording.getBucket())
                .s3Key(recording.getS3Key())
                .status(valueOf(recording.getStatus()))
                .sizeBytes(recording.getSizeBytes())
                .durationSeconds(recording.getDurationSeconds())
                .source(recording.getSource())
                .createdAt(recording.getCreatedAt())
                .assembledAt(recording.getAssembledAt())
                .build();
    }


    private static AssessmentRequiredStreamType streamTypeFromString(String value) {
        return value == null ? null : AssessmentRequiredStreamType.valueOf(value);
    }

    private static AssessmentRecordingAssemblyStatus statusFromString(String value) {
        return value == null ? null : AssessmentRecordingAssemblyStatus.valueOf(value);
    }

    private static String valueOf(AssessmentRequiredStreamType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(AssessmentRecordingAssemblyStatus status) {
        return status == null ? null : status.name();
    }
}
