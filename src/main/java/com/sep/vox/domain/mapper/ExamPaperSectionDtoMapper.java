package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.domain.model.exam.ExamPaperSection;

public final class ExamPaperSectionDtoMapper {

    private ExamPaperSectionDtoMapper() {
    }

    public static ExamPaperSectionDto toDto(ExamPaperSection domain) {
        return new ExamPaperSectionDto(
            domain.getId(),
            domain.getPaperId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getWeight(),
            domain.getSectionTimeLimitSeconds()
        );
    }

    public static List<ExamPaperSectionDto> toDtoList(List<ExamPaperSection> domains) {
        return domains.stream()
            .map(ExamPaperSectionDtoMapper::toDto)
            .toList();
    }
}
