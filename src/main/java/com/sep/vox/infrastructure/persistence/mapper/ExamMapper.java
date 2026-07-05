package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public final class ExamMapper {

    public static Exam toDomain(ExamJpaEntity jpa) {
        return new Exam(
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
            jpa.getOpenAt(),
            jpa.getCloseAt(),
            jpa.getAssessmentPolicyId(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamJpaEntity toJpa(Exam exam) {
        return new ExamJpaEntity(
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
            exam.getOpenAt(),
            exam.getCloseAt(),
            exam.getAssessmentPolicyId(),
            exam.getCreatedAt(),
            exam.getUpdatedAt(),
            exam.getCreatedBy(),
            exam.getUpdatedBy()
        );
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
}
