package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.model.exam.ExamPaperItem;

public final class ExamPaperItemDtoMapper {

    private ExamPaperItemDtoMapper() {
    }

    public static ExamPaperItemDto toDto(ExamPaperItem domain) {
        return new ExamPaperItemDto(
            domain.getId(),
            domain.getBlueprintSlotId(),
            domain.getSectionId(),
            domain.getPaperId(),
            domain.getQuestionId(),
            domain.getOrder(),
            domain.getWeight()
        );
    }

    public static List<ExamPaperItemDto> toDtoList(List<ExamPaperItem> domains) {
        return domains.stream()
            .map(ExamPaperItemDtoMapper::toDto)
            .toList();
    }
}
