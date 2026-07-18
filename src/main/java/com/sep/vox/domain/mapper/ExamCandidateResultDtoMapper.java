package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamCandidateResultDto;
import com.sep.vox.domain.model.exam.ExamCandidateResult;

public final class ExamCandidateResultDtoMapper {

    private ExamCandidateResultDtoMapper() {
    }

    public static ExamCandidateResultDto toDto(ExamCandidateResult domain) {
        return new ExamCandidateResultDto(
            domain.getId(),
            domain.getExamId(),
            domain.getTotalScore(),
            domain.getStatus() == null ? null : domain.getStatus().name(),
            valueOf(domain.getReleasedAt()),
            valueOf(domain.getFinalizedAt()),
            valueOf(domain.getCreatedAt())
        );
    }

    public static PageResult<ExamCandidateResultDto> toDtoPage(PageResult<ExamCandidateResult> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    public static List<ExamCandidateResultDto> toDtoList(List<ExamCandidateResult> domains) {
        return domains.stream()
            .map(ExamCandidateResultDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
