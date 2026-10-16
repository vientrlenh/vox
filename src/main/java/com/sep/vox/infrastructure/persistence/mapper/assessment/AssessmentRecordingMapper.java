package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentRecordingJpaEntity;

public final class AssessmentRecordingMapper {
    
    public static AssessmentRecording toDomain(AssessmentRecordingJpaEntity jpa) {
        return new AssessmentRecording(
            jpa.getId(), 
            jpa.getAssessmentSessionId(), 
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

    public static AssessmentRecordingJpaEntity toJpa(AssessmentRecording recording) {
        return new AssessmentRecordingJpaEntity(
            recording.getId(), 
            recording.getAssessmentSessionId(), 
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
