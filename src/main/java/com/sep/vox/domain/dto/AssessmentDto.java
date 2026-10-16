package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.shared.PageResult;

public record AssessmentDto(
    UUID id,
    UUID blueprintId,
    UUID blueprintVersionId,
    String code,
    String name,
    String description,
    UUID schoolId,
    UUID languageId,
    AssessmentKind kind,
    AssessmentDeliveryMode deliveryMode,
    AssessmentStatus status,
    Integer maxAttempt,
    Integer timeDurationSecond,
    AssessmentResultDecisionMethod resultDecisionMethod,
    Instant openAt,
    Instant closeAt,
    UUID assessmentPolicyId,
    boolean requiresOtp,
    AssessmentRequiredStreamType requiredStreamType,
    AssessmentStreamTypePermission streamTypePermission,
    Boolean papersLocked,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,

    /** Ngưỡng tin cậy AI theo phần trăm (0-100); null = nhà trường không đặt. */
    BigDecimal aiConfidenceThresholdPercent
) {

    public static AssessmentDto toDto(Assessment assessment) {
        return toDto(assessment, null);
    }

    public static AssessmentDto toDto(Assessment assessment, Boolean papersLocked) {
        return new AssessmentDto(
            assessment.getId(),
            assessment.getBlueprintId(),
            assessment.getBlueprintVersionId(),
            assessment.getCode(),
            assessment.getName(),
            assessment.getDescription(),
            assessment.getSchoolId(),
            assessment.getLanguageId(),
            assessment.getKind(),
            assessment.getDeliveryMode(),
            assessment.getStatus(),
            assessment.getMaxAttempt(),
            assessment.getTimeDurationSecond(),
            assessment.getResultDecisionMethod(),
            assessment.getOpenAt(),
            assessment.getCloseAt(),
            assessment.getAssessmentPolicyId(),
            assessment.isRequiresOtp(),
            assessment.getRequiredStreamType(),
            assessment.getStreamTypePermission(),
            papersLocked,
            assessment.getCreatedAt(),
            assessment.getUpdatedAt(),
            assessment.getCreatedBy(),
            assessment.getUpdatedBy(),
            assessment.getAiConfidenceThresholdPercent()
        );
    }

    public static PageResult<AssessmentDto> toDtoPage(PageResult<Assessment> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    public static List<AssessmentDto> toDtoList(List<Assessment> domains) {
        return domains.stream()
            .map(AssessmentDto::toDto)
            .toList();
    }
}
