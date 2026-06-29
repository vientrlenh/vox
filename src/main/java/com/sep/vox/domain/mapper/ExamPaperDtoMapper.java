package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.model.exam.ExamPaper;

public final class ExamPaperDtoMapper {

    private ExamPaperDtoMapper() {
    }

    public static ExamPaperDto toDto(ExamPaper domain) {
        return new ExamPaperDto(
            domain.getId(),
            domain.getExamId(),
            domain.getCode(),
            domain.getVariant(),
            domain.getStatus().name(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<ExamPaperDto> toDtoList(List<ExamPaper> domains) {
        return domains.stream()
            .map(ExamPaperDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
