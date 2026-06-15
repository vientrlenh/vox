package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.model.rubric.RubricResultBand;

import java.util.List;

public class RubricResultBandDtoMapper {

    public static RubricResultBandDto toDto(RubricResultBand band) {
        if (band == null) {
            return null;
        }
        return new RubricResultBandDto(
                band.getId(),
                band.getRubricVersionId(),
                band.getCode(),
                band.getName(),
                band.getDescription(),
                band.getScoreMin(),
                band.getScoreMax(),
                band.getOrder(),
                band.getCreatedAt(),
                band.getUpdatedAt(),
                band.getCreatedBy(),
                band.getUpdatedBy()
        );
    }

    public static List<RubricResultBandDto> toDtoList(List<RubricResultBand> bands) {
        return bands.stream()
                .map(RubricResultBandDtoMapper::toDto)
                .toList();
    }

    public static PageResult<RubricResultBandDto> toPage(PageResult<RubricResultBand> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}