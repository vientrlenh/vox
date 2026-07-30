package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.valueobject.FrameworkCode;

public final class FrameworkDtoMapper {

    public static FrameworkDto toDto(Framework framework) {
        return new FrameworkDto(
            framework.getId(),
            valueOf(framework.getCode()),
            framework.getName(),
            framework.getDescription(),
            framework.isActive(),
            valueOf(framework.getCreatedAt()),
            valueOf(framework.getUpdatedAt())
        );
    }

    public static List<FrameworkDto> toDtoList(List<Framework> list) {
        return list.stream().map(FrameworkDtoMapper::toDto).toList();
    }

    public static PageResult<FrameworkDto> toDtoPage(PageResult<Framework> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static String valueOf(FrameworkCode code) {
        return code == null ? null : code.value();
    }

    private static String valueOf(Instant dt) {
        return dt == null ? null : dt.toString();
    }
}
