package com.sep.vox.infrastructure.persistence.mapper.assessment;

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
        var exam = new Assessment(
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
        exam.setAssessmentTimeDurationSecond(jpa.getAssessmentTimeDurationSecond());
        // Đặt qua setter chứ không nhét vào constructor: constructor của Assessment đã dài và có nhiều
        // biến thể, thêm tham số vào là sửa mọi nơi gọi chỉ để mang một trường tuỳ chọn.
        exam.setAiConfidenceThresholdPercent(jpa.getAiConfidenceThresholdPercent());
        exam.setHumanGradingNotifiedAt(jpa.getHumanGradingNotifiedAt());
        return exam;
    }

    public static AssessmentJpaEntity toJpa(Assessment exam) {
        var jpa = new AssessmentJpaEntity(
            exam.getId(),
            exam.getBlueprintId(),
            exam.getBlueprintVersionId(),
            exam.getCode(),
            exam.getName(),
            exam.getDescription(),
            exam.getSchoolId(),
            exam.getLanguageId(),
            exam.getKind().name(),
            exam.getDeliveryMode() == null ? null : exam.getDeliveryMode().name(),
            exam.getStatus().name(),
            exam.getMaxAttempt(),
            exam.getAssessmentTimeDurationSecond(),
            exam.getResultDecisionMethod() == null ? null : exam.getResultDecisionMethod().name(),
            valueOf(exam.getRequiredStreamType()), 
            valueOf(exam.getStreamTypePermission()), 
            exam.getOpenAt(),
            exam.getCloseAt(),
            exam.getAssessmentPolicyId(),
            exam.isRequiresOtp(),
            exam.getCreatedAt(),
            exam.getUpdatedAt(),
            exam.getCreatedBy(),
            exam.getUpdatedBy()
        );
        jpa.setAssessmentTimeDurationSecond(exam.getAssessmentTimeDurationSecond());
        jpa.setHumanGradingNotifiedAt(exam.getHumanGradingNotifiedAt());
        jpa.setAiConfidenceThresholdPercent(exam.getAiConfidenceThresholdPercent());
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
