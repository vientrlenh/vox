package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public final class ExamMapper {

    public static Exam toDomain(ExamJpaEntity jpa) {
        var exam = new Exam(
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
            jpa.getExamTimeDurationSecond(),
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
        exam.setExamTimeDurationSecond(jpa.getExamTimeDurationSecond());
        // Đặt qua setter chứ không nhét vào constructor: constructor của Exam đã dài và có nhiều
        // biến thể, thêm tham số vào là sửa mọi nơi gọi chỉ để mang một trường tuỳ chọn.
        exam.setAiConfidenceThresholdPercent(jpa.getAiConfidenceThresholdPercent());
        return exam;
    }

    public static ExamJpaEntity toJpa(Exam exam) {
        var jpa = new ExamJpaEntity(
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
            exam.getExamTimeDurationSecond(),
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
        jpa.setExamTimeDurationSecond(exam.getExamTimeDurationSecond());
        jpa.setAiConfidenceThresholdPercent(exam.getAiConfidenceThresholdPercent());
        return jpa;
    }

    private static ExamStatus statusFromString(String status) {
        return status == null ? null : ExamStatus.valueOf(status);
    }

    private static ExamKind kindFromString(String kind) {
        return kind == null ? null : ExamKind.valueOf(kind);
    }

    private static ExamDeliveryMode deliveryModeFromString(String deliveryMode) {
        return deliveryMode == null ? null : ExamDeliveryMode.valueOf(deliveryMode);
    }

    private static ResultDecisionMethod resultDecisionMethodFromString(String resultDecisionMethod) {
        return resultDecisionMethod == null ? null : ResultDecisionMethod.valueOf(resultDecisionMethod);
    }

    private static ExamRequiredStreamType requiredStreamTypeFromString(String type) {
        return type == null ? null : ExamRequiredStreamType.valueOf(type);
    }

    private static String valueOf(ExamRequiredStreamType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ExamStreamTypePermission permission) {
        return permission == null ? null : permission.name();
    }

    private static ExamStreamTypePermission streamTypePermissionFromString(String permission) {
        return permission == null ? null : ExamStreamTypePermission.valueOf(permission);
    }
}
