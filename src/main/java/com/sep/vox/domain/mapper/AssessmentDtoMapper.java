package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;

public final class AssessmentDtoMapper {

    private AssessmentDtoMapper() {
    }

    public static AssessmentDto toDto(Assessment domain) {
        return toDto(domain, null);
    }

    public static AssessmentDto toDto(Assessment domain, Boolean papersLocked) {
        return new AssessmentDto(
            domain.getId(),
            domain.getBlueprintId(),
            domain.getBlueprintVersionId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.getSchoolId(),
            domain.getLanguageId(),
            domain.getKind().name(),
            domain.getDeliveryMode() == null ? null : domain.getDeliveryMode().name(),
            domain.getStatus().name(),
            domain.getMaxAttempt(),
            domain.getAssessmentTimeDurationSecond(),
            domain.getResultDecisionMethod() == null ? null : domain.getResultDecisionMethod().name(),
            valueOf(domain.getOpenAt()),
            valueOf(domain.getCloseAt()),
            domain.getAssessmentPolicyId(),
            domain.isRequiresOtp(),
            domain.getRequiredStreamType() == null ? null : domain.getRequiredStreamType().name(),
            domain.getStreamTypePermission() == null ? null : domain.getStreamTypePermission().name(),
            papersLocked,
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy(),
            domain.getAiConfidenceThresholdPercent()
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
            .map(AssessmentDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
