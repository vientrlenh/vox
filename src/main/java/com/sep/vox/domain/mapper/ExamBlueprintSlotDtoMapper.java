package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;

public final class ExamBlueprintSlotDtoMapper {

    private ExamBlueprintSlotDtoMapper() {
    }

    public static ExamBlueprintSlotDto toDto(ExamBlueprintSlot domain) {
        return new ExamBlueprintSlotDto(
            domain.getId(),
            domain.getSectionId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getWeight(),
            domain.getPrepTimeSecondsOverride(),
            domain.getResponseTimeSecondsOverride(),
            domain.getSlotType().name(),
            domain.getFixedQuestionId(),
            QuestionSelectionSpecDtoMapper.toDto(domain.getSelectionSpec()),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<ExamBlueprintSlotDto> toDtoList(List<ExamBlueprintSlot> domains) {
        return domains.stream()
            .map(ExamBlueprintSlotDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
