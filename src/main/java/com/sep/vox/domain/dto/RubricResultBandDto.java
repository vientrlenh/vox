package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;

public record RubricResultBandDto(
    UUID id,
    UUID rubricVersionId,
    String name,
    String description,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    Integer order
) {

    public static RubricResultBandDto toDto(RubricResultBand rubricResultBand) {
        return new RubricResultBandDto(
            rubricResultBand.getId(),
            rubricResultBand.getRubricVersionId(),
            rubricResultBand.getName(),
            rubricResultBand.getDescription(),
            PositiveDecimal.valueOf(rubricResultBand.getScoreMin()),
            PositiveDecimal.valueOf(rubricResultBand.getScoreMax()),
            PositiveInteger.valueOf(rubricResultBand.getOrder())
        );
    }

    public static List<RubricResultBandDto> toDtoList(List<RubricResultBand> bands) {
        return bands.stream()
                .map(RubricResultBandDto::toDto)
                .toList();
    }

    public static PageResult<RubricResultBandDto> toDtoPage(PageResult<RubricResultBand> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
