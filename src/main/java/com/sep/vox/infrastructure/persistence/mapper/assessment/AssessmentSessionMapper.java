package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;

public final class AssessmentSessionMapper {

    private AssessmentSessionMapper() {}

    public static AssessmentSession toDomain(AssessmentSessionJpaEntity jpa) {
        var domain = new AssessmentSession(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getCandidateId(),
            jpa.getPaperId(),
            jpa.getStartedAt(),
            jpa.getSubmittedAt(),
            statusFromString(jpa.getStatus()),
            jpa.isFlagged(),
            jpa.getFlagReason()
        );
        domain.setChosenStreamType(chosenStreamTypeFromString(jpa.getChosenStreamType()));
        domain.setRemainingSeconds(jpa.getRemainingSeconds());
        domain.setGradingError(jpa.getGradingError());
        domain.setGradingRetryCount(jpa.getGradingRetryCount());
        domain.setSchoolRegradeCount(jpa.getSchoolRegradeCount());
        return domain;
    }

    public static AssessmentSessionJpaEntity toJpa(AssessmentSession domain) {
        var jpa = new AssessmentSessionJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getCandidateId(),
            domain.getPaperId(),
            domain.getStartedAt(),
            domain.getSubmittedAt(),
            domain.getStatus().name(),
            domain.isFlagged(),
            domain.getFlagReason()
        );
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
