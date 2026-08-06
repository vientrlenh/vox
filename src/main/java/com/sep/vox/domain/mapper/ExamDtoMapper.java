package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.model.exam.Exam;

public final class ExamDtoMapper {

    private ExamDtoMapper() {
    }

    public static ExamDto toDto(Exam domain) {
        return toDto(domain, null);
    }

    public static ExamDto toDto(Exam domain, Boolean papersLocked) {
        return new ExamDto(
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
            domain.getExamTimeDurationSecond(),
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
            domain.getUpdatedBy()
        );
    }

    public static PageResult<ExamDto> toDtoPage(PageResult<Exam> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    public static List<ExamDto> toDtoList(List<Exam> domains) {
        return domains.stream()
            .map(ExamDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
