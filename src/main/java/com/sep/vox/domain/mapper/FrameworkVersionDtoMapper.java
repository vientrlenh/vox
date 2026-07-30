package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.model.framework.FrameworkVersion;

public final class FrameworkVersionDtoMapper {

    public static FrameworkVersionDto toDto(FrameworkVersion version) {
        return new FrameworkVersionDto(
            version.getId(),
            version.getFrameworkId(),
            version.getCode(),
            version.getName(),
            version.getDescription(),
            version.getVersion(),
            valueOf(version.getEffectiveFrom()),
            valueOf(version.getEffectiveTo()),
            version.getStatus().name(),
            valueOf(version.getCreatedAt()),
            valueOf(version.getUpdatedAt()),
            List.of(),
            List.of()
        );
    }

    public static PageResult<FrameworkVersionDto> toDtoPage(PageResult<FrameworkVersion> page) {
        return new PageResult<>(
            page.content().stream().map(FrameworkVersionDtoMapper::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(Instant dt) {
        return dt == null ? null : dt.toString();
    }
}
