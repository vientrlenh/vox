package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;

public final class AssessmentMapper {

    public static Assessment toDomain(AssessmentJpaEntity jpa) {
        Assessment assessment = Assessment.builder()
                .id(jpa.getId())
                .blueprintId(jpa.getBlueprintId())
                .blueprintVersionId(jpa.getBlueprintVersionId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .schoolId(jpa.getSchoolId())
                .languageId(jpa.getLanguageId())
                .kind(kindFromString(jpa.getKind()))
                .deliveryMode(deliveryModeFromString(jpa.getDeliveryMode()))
                .status(statusFromString(jpa.getStatus()))
                .maxAttempt(jpa.getMaxAttempt())
                .assessmentTimeDurationSecond(jpa.getAssessmentTimeDurationSecond())
                .resultDecisionMethod(resultDecisionMethodFromString(jpa.getResultDecisionMethod()))
                .requiredStreamType(requiredStreamTypeFromString(jpa.getRequiredStreamType()))
                .streamTypePermission(streamTypePermissionFromString(jpa.getStreamTypePermission()))
                .openAt(jpa.getOpenAt())
                .closeAt(jpa.getCloseAt())
                .assessmentPolicyId(jpa.getAssessmentPolicyId())
                .requiresOtp(jpa.isRequiresOtp())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        assessment.setAssessmentTimeDurationSecond(jpa.getAssessmentTimeDurationSecond());
        // Set through a setter instead of the constructor: the Assessment constructor is already long and has many
        // variants, and adding a parameter would mean editing every caller just to carry one optional field.
        assessment.setAiConfidenceThresholdPercent(jpa.getAiConfidenceThresholdPercent());
        assessment.setHumanGradingNotifiedAt(jpa.getHumanGradingNotifiedAt());
        return assessment;
    }

    public static AssessmentJpaEntity toJpa(Assessment assessment) {
        AssessmentJpaEntity jpa = AssessmentJpaEntity.builder()
                .id(assessment.getId())
                .blueprintId(assessment.getBlueprintId())
                .blueprintVersionId(assessment.getBlueprintVersionId())
                .code(assessment.getCode())
                .name(assessment.getName())
                .description(assessment.getDescription())
                .schoolId(assessment.getSchoolId())
                .languageId(assessment.getLanguageId())
                .kind(assessment.getKind().name())
                .deliveryMode(assessment.getDeliveryMode() == null ? null : assessment.getDeliveryMode().name())
                .status(assessment.getStatus().name())
                .maxAttempt(assessment.getMaxAttempt())
                .assessmentTimeDurationSecond(assessment.getAssessmentTimeDurationSecond())
                .resultDecisionMethod(assessment.getResultDecisionMethod() == null ? null : assessment.getResultDecisionMethod().name())
                .requiredStreamType(valueOf(assessment.getRequiredStreamType()))
                .streamTypePermission(valueOf(assessment.getStreamTypePermission()))
                .openAt(assessment.getOpenAt())
                .closeAt(assessment.getCloseAt())
                .assessmentPolicyId(assessment.getAssessmentPolicyId())
                .requiresOtp(assessment.isRequiresOtp())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .createdBy(assessment.getCreatedBy())
                .updatedBy(assessment.getUpdatedBy())
                .build();
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
