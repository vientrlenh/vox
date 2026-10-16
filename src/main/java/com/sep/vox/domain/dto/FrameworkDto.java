package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;

public record FrameworkDto(
    UUID id,
    String code,
    String name,
    String description,
    FrameworkStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static FrameworkDto toDto(Framework framework) {
        return new FrameworkDto(
            framework.getId(),
            Code.valueOf(framework.getCode()),
            framework.getName(),
            framework.getDescription(),
            framework.getStatus(),
            framework.getCreatedAt(),
            framework.getUpdatedAt()
        );
    }

    public static List<FrameworkDto> toDtoList(List<Framework> list) {
        return list.stream().map(FrameworkDto::toDto).toList();
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
}
