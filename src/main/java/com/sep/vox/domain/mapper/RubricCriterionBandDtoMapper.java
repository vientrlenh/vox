package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionBandDto;
import com.sep.vox.domain.model.rubric.RubricCriterionBand;

import java.util.List;

public class RubricCriterionBandDtoMapper {

    public static RubricCriterionBandDto toDto(RubricCriterionBand band) {
        if (band == null) {
            return null;
        }
        return new RubricCriterionBandDto(
                band.getId(),
                band.getCriterionId(),
                band.getCode(),
                band.getScoreMin(),
                band.getScoreMax(),
                band.getCreatedAt(),
                band.getUpdatedAt(),
                band.getCreatedBy(),
                band.getUpdatedBy()
        );
    }

    public static List<RubricCriterionBandDto> toDtoList(List<RubricCriterionBand> bands) {
        return bands.stream()
                .map(RubricCriterionBandDtoMapper::toDto)
                .toList();
    }

    public static PageResult<RubricCriterionBandDto> toPage(PageResult<RubricCriterionBand> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}