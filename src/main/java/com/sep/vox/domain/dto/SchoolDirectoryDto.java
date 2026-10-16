package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.SchoolDomain;

public record SchoolDirectoryDto(
    UUID id,
    String code,
    String name,
    UUID wardId,
    UUID cityId,
    UUID provinceId,
    String domain,
    String streetAddress,
    SchoolDirectoryOrigin origin,
    boolean verified,
    Instant createdAt,
    Instant updatedAt
) {

    public static SchoolDirectoryDto toDto(SchoolDirectory schoolDirectory) {
        return new SchoolDirectoryDto(
            schoolDirectory.getId(),
            Code.valueOf(schoolDirectory.getCode()),
            schoolDirectory.getName(),
            schoolDirectory.getWardId(),
            schoolDirectory.getCityId(),
            schoolDirectory.getProvinceId(),
            SchoolDomain.valueOf(schoolDirectory.getDomain()),
            schoolDirectory.getStreetAddress(),
            schoolDirectory.getOrigin(),
            schoolDirectory.isVerified(),
            schoolDirectory.getCreatedAt(),
            schoolDirectory.getUpdatedAt()
        );
    }

    public static List<SchoolDirectoryDto> toDtoList(List<SchoolDirectory> dirs) {
        return dirs.stream()
            .map(SchoolDirectoryDto::toDto)
            .toList();
    }

    public static PageResult<SchoolDirectoryDto> toDtoPage(PageResult<SchoolDirectory> directoryPage) {
        return new PageResult<>(
            toDtoList(directoryPage.content()), 
            directoryPage.page(), 
            directoryPage.size(), 
            directoryPage.totalElements(), 
            directoryPage.totalPages()
        );
    }
}
