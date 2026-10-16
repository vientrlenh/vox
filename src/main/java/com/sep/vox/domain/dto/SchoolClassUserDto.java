package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.shared.PageResult;

public record SchoolClassUserDto(
    UUID id,
    UUID userId,
    UUID schoolClassId,
    Instant joinAt,
    Instant leftAt,
    UUID assignedBy
) {

    public static SchoolClassUserDto toDto(SchoolClassUser schoolClassUser) {
        return new SchoolClassUserDto(
            schoolClassUser.getId(),
            schoolClassUser.getUserId(),
            schoolClassUser.getSchoolClassId(),
            schoolClassUser.getJoinAt(),
            schoolClassUser.getLeftAt(),
            schoolClassUser.getAssignedBy()
        );
    }

    public static List<SchoolClassUserDto> toDtoList(List<SchoolClassUser> schoolClassUsers) {
        return schoolClassUsers.stream()
            .map(SchoolClassUserDto::toDto)
            .toList();
    }

    public static PageResult<SchoolClassUserDto> toDtoPage(PageResult<SchoolClassUser> schoolClassUserPage) {
        return new PageResult<>(
            toDtoList(schoolClassUserPage.content()), 
            schoolClassUserPage.page(), 
            schoolClassUserPage.size(), 
            schoolClassUserPage.totalElements(), 
            schoolClassUserPage.totalPages()
        );
    }
}
