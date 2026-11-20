package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.infrastructure.persistence.entity.AssessmentJpaEntity;

public final class AssessmentMapper {

    public static Assessment toDomain(AssessmentJpaEntity jpa) {
        Assessment assessment = new Assessment(
            jpa.getId(),
            jpa.getBlueprintId(),
            jpa.getBlueprintVersionId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getSchoolId(),
            jpa.getLanguageId(),
            kindFromString(jpa.getKind()),
            deliveryModeFromString(jpa.getDeliveryMode()),
            statusFromString(jpa.getStatus()),
            jpa.getMaxAttempt(),
            jpa.getAssessmentTimeDurationSecond(),
            resultDecisionMethodFromString(jpa.getResultDecisionMethod()),
            requiredStreamTypeFromString(jpa.getRequiredStreamType()), 
            streamTypePermissionFromString(jpa.getStreamTypePermission()), 
            jpa.getOpenAt(),
            jpa.getCloseAt(),
            jpa.getAssessmentPolicyId(),
            jpa.isRequiresOtp(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
        assessment.setAssessmentTimeDurationSecond(jpa.getAssessmentTimeDurationSecond());
        // Set through a setter instead of the constructor: the Assessment constructor is already long and has many
        // variants, and adding a parameter would mean editing every caller just to carry one optional field.
        assessment.setAiConfidenceThresholdPercent(jpa.getAiConfidenceThresholdPercent());
        assessment.setHumanGradingNotifiedAt(jpa.getHumanGradingNotifiedAt());
        return assessment;
    }

    public static AssessmentJpaEntity toJpa(Assessment assessment) {
        AssessmentJpaEntity jpa = new AssessmentJpaEntity(
            assessment.getId(),
            assessment.getBlueprintId(),
            assessment.getBlueprintVersionId(),
            assessment.getCode(),
            assessment.getName(),
            assessment.getDescription(),
            assessment.getSchoolId(),
            assessment.getLanguageId(),
            assessment.getKind().name(),
            assessment.getDeliveryMode() == null ? null : assessment.getDeliveryMode().name(),
            assessment.getStatus().name(),
            assessment.getMaxAttempt(),
            assessment.getAssessmentTimeDurationSecond(),
            assessment.getResultDecisionMethod() == null ? null : assessment.getResultDecisionMethod().name(),
            valueOf(assessment.getRequiredStreamType()), 
            valueOf(assessment.getStreamTypePermission()), 
            assessment.getOpenAt(),
            assessment.getCloseAt(),
            assessment.getAssessmentPolicyId(),
            assessment.isRequiresOtp(),
            assessment.getCreatedAt(),
            assessment.getUpdatedAt(),
            assessment.getCreatedBy(),
            assessment.getUpdatedBy()
        );
        jpa.setAssessmentTimeDurationSecond(assessment.getAssessmentTimeDurationSecond());
        jpa.setHumanGradingNotifiedAt(assessment.getHumanGradingNotifiedAt());
        jpa.setAiConfidenceThresholdPercent(assessment.getAiConfidenceThresholdPercent());
        return jpa;
    }

    private static AssessmentStatus statusFromString(String status) {
        return status == null ? null : AssessmentStatus.valueOf(status);
    }

    private static AssessmentKind kindFromString(String kind) {
        return kind == null ? null : AssessmentKind.valueOf(kind);
    }

    private static AssessmentDeliveryMode deliveryModeFromString(String deliveryMode) {
        return deliveryMode == null ? null : AssessmentDeliveryMode.valueOf(deliveryMode);
    }

    private static ResultDecisionMethod resultDecisionMethodFromString(String resultDecisionMethod) {
        return resultDecisionMethod == null ? null : ResultDecisionMethod.valueOf(resultDecisionMethod);
    }

    private static AssessmentRequiredStreamType requiredStreamTypeFromString(String type) {
        return type == null ? null : AssessmentRequiredStreamType.valueOf(type);
    }

    private static String valueOf(AssessmentRequiredStreamType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(AssessmentStreamTypePermission permission) {
        return permission == null ? null : permission.name();
    }

    private static AssessmentStreamTypePermission streamTypePermissionFromString(String permission) {
        return permission == null ? null : AssessmentStreamTypePermission.valueOf(permission);
    }
}
