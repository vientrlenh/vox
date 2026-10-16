package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveLong;

public record FrameworkVersionDto(
    UUID id,
    UUID frameworkId,
    String code,
    String name,
    String description,
    Long version,
    Instant effectiveFrom,
    Instant effectiveTo,
    FrameworkVersionStatus status,
    Instant createdAt,
    Instant updatedAt,
    List<FrameworkCriterionDto> criteria,
    List<FrameworkResultBandDto> resultBands
) {

    public static FrameworkVersionDto toDto(FrameworkVersion frameworkVersion) {
        return new FrameworkVersionDto(
            frameworkVersion.getId(),
            frameworkVersion.getFrameworkId(),
            Code.valueOf(frameworkVersion.getCode()),
            frameworkVersion.getName(),
            frameworkVersion.getDescription(),
            PositiveLong.valueOf(frameworkVersion.getVersion()),
            frameworkVersion.getEffectiveFrom(),
            frameworkVersion.getEffectiveTo(),
            frameworkVersion.getStatus(),
            frameworkVersion.getCreatedAt(),
            frameworkVersion.getUpdatedAt(),
            List.of(),
            List.of()
        );
    }

    public static PageResult<FrameworkVersionDto> toDtoPage(PageResult<FrameworkVersion> page) {
        return new PageResult<>(
            page.content().stream().map(FrameworkVersionDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
