package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.model.school.SchoolClassUser;

public final class SchoolClassUserDtoMapper {
    

    public static SchoolClassUserDto toSchoolClassUserDto(SchoolClassUser schoolClassUser) {
        return new SchoolClassUserDto(
            schoolClassUser.getId(),
            schoolClassUser.getUserId(),
            schoolClassUser.getSchoolClassId(),
            schoolClassUser.isActive(),
            valueOf(schoolClassUser.getJoinedAt()),
            valueOf(schoolClassUser.getLeftAt()),
            schoolClassUser.getAssignedBy()
        );
    }

    public static List<SchoolClassUserDto> toSchoolClassUserDtoList(List<SchoolClassUser> schoolClassUsers) {
        return schoolClassUsers.stream()
            .map(SchoolClassUserDtoMapper::toSchoolClassUserDto)
            .toList();
    }

    public static PageResult<SchoolClassUserDto> toSchoolClassUserDtoPage(PageResult<SchoolClassUser> schoolClassUserPage) {
        return new PageResult<>(
            toSchoolClassUserDtoList(schoolClassUserPage.content()), 
            schoolClassUserPage.page(), 
            schoolClassUserPage.size(), 
            schoolClassUserPage.totalElements(), 
            schoolClassUserPage.totalPages()
        );
    }

    private static String valueOf(Instant dateTime) {
        return dateTime == null ? null : dateTime.toString();
    } 
}
