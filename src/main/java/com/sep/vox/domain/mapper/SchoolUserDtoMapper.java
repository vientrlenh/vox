package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.school.SchoolUser;

public final class SchoolUserDtoMapper {
    
    public static SchoolUserDto toSchoolUserDto(SchoolUser schoolUser) {
        return new SchoolUserDto(
            schoolUser.getId(),  
            schoolUser.getSchoolId(), 
            schoolUser.getUserId(), 
            schoolUser.getStartDate().toString(), 
            valueOf(schoolUser.getEndDate())
        );
    }

    public static List<SchoolUserDto> toSchoolUserListDto(List<SchoolUser> schoolUsers) {
        return schoolUsers.stream()
            .map(SchoolUserDtoMapper::toSchoolUserDto)
            .toList();
    }

    public static PageResult<SchoolUserDto> toSchoolUserPageDto(PageResult<SchoolUser> schoolUserPage) {
        return new PageResult<>(
            toSchoolUserListDto(schoolUserPage.content()), 
            schoolUserPage.page(), 
            schoolUserPage.size(), 
            schoolUserPage.totalElements(), 
            schoolUserPage.totalPages()
        );
    }

    private static String valueOf(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
