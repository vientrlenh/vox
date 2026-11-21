package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;

public final class AssessmentSessionMapper {

    private AssessmentSessionMapper() {}

    public static AssessmentSession toDomain(AssessmentSessionJpaEntity jpa) {
        AssessmentSession domain = AssessmentSession.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .candidateId(jpa.getCandidateId())
                .paperId(jpa.getPaperId())
                .startedAt(jpa.getStartedAt())
                .submittedAt(jpa.getSubmittedAt())
                .status(statusFromString(jpa.getStatus()))
                .flagged(jpa.isFlagged())
                .flagReason(jpa.getFlagReason())
                .build();
        domain.setChosenStreamType(chosenStreamTypeFromString(jpa.getChosenStreamType()));
        domain.setRemainingSeconds(jpa.getRemainingSeconds());
        domain.setGradingError(jpa.getGradingError());
        domain.setGradingRetryCount(jpa.getGradingRetryCount());
        domain.setSchoolRegradeCount(jpa.getSchoolRegradeCount());
        return domain;
    }

    public static AssessmentSessionJpaEntity toJpa(AssessmentSession domain) {
        AssessmentSessionJpaEntity jpa = AssessmentSessionJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .candidateId(domain.getCandidateId())
                .paperId(domain.getPaperId())
                .startedAt(domain.getStartedAt())
                .submittedAt(domain.getSubmittedAt())
                .status(domain.getStatus().name())
                .flagged(domain.isFlagged())
                .flagReason(domain.getFlagReason())
                .build();
        jpa.setChosenStreamType(
            domain.getChosenStreamType() == null ? null : domain.getChosenStreamType().name()
        );
        jpa.setRemainingSeconds(domain.getRemainingSeconds());
        jpa.setGradingError(domain.getGradingError());
        jpa.setGradingRetryCount(domain.getGradingRetryCount());
        jpa.setSchoolRegradeCount(domain.getSchoolRegradeCount());
        return jpa;
    }

    private static AssessmentSessionStatus statusFromString(String status) {
        return status == null ? null : AssessmentSessionStatus.valueOf(status);
    }

    private static AssessmentRequiredStreamType chosenStreamTypeFromString(String chosenStreamType) {
        return chosenStreamType == null ? null : AssessmentRequiredStreamType.valueOf(chosenStreamType);
    }
}
