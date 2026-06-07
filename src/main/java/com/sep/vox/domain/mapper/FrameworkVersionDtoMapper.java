package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkResultBandStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;

public final class FrameworkVersionDtoMapper {

    public static FrameworkVersionDto toDto(FrameworkVersion version, List<FrameworkCriterion> criteria,
            List<FrameworkCriterionBand> allBands, List<FrameworkResultBand> resultBands) {
        var criterionDtos = criteria.stream()
            .map(c -> toCriterionDto(c, allBands))
            .toList();
        var resultBandDtos = resultBands.stream().map(FrameworkVersionDtoMapper::toResultBandDto).toList();
        return new FrameworkVersionDto(
            version.getId(),
            version.getFrameworkId(),
            version.getCode(),
            version.getName(),
            version.getDescription(),
            version.getVersion(),
            valueOf(version.getEffectiveFrom()),
            valueOf(version.getEffectiveTo()),
            version.getStatus() == null ? null : version.getStatus().name(),
            valueOf(version.getCreatedAt()),
            valueOf(version.getUpdatedAt()),
            criterionDtos,
            resultBandDtos
        );
    }

    public static FrameworkVersionDto toDtoShallow(FrameworkVersion version) {
        return new FrameworkVersionDto(
            version.getId(),
            version.getFrameworkId(),
            version.getCode(),
            version.getName(),
            version.getDescription(),
            version.getVersion(),
            valueOf(version.getEffectiveFrom()),
            valueOf(version.getEffectiveTo()),
            version.getStatus() == null ? null : version.getStatus().name(),
            valueOf(version.getCreatedAt()),
            valueOf(version.getUpdatedAt()),
            List.of(),
            List.of()
        );
    }

    public static PageResult<FrameworkVersionDto> toDtoPage(PageResult<FrameworkVersion> page) {
        return new PageResult<>(
            page.content().stream().map(FrameworkVersionDtoMapper::toDtoShallow).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static FrameworkCriterionDto toCriterionDto(FrameworkCriterion criterion, List<FrameworkCriterionBand> allBands) {
        var bandDtos = allBands.stream()
            .filter(b -> b.getFrameworkCriterionId().equals(criterion.getId()))
            .map(FrameworkVersionDtoMapper::toCriterionBandDto)
            .toList();
        return new FrameworkCriterionDto(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            bandDtos
        );
    }

    private static FrameworkCriterionBandDto toCriterionBandDto(FrameworkCriterionBand band) {
        return new FrameworkCriterionBandDto(
            band.getId(),
            band.getFrameworkCriterionId(),
            band.getFrameworkResultBandId(),
            band.getDescriptor(),
            band.getPositiveSignals(),
            band.getNegativeSignals()
        );
    }

    private static FrameworkResultBandDto toResultBandDto(FrameworkResultBand band) {
        return new FrameworkResultBandDto(
            band.getId(),
            band.getFrameworkVersionId(),
            band.getCode(),
            band.getLabel(),
            band.getDescription(),
            band.getScoreMin(),
            band.getScoreMax(),
            band.getOrder(),
            valueOf(band.getStatus())
        );
    }

    private static String valueOf(FrameworkResultBandStatus status) {
        return status == null ? null : status.name();
    }

    private static String valueOf(OffsetDateTime dt) {
        return dt == null ? null : dt.toString();
    }
}
