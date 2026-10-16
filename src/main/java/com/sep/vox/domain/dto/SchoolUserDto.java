package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.shared.PageResult;

public record SchoolUserDto(
    UUID id,
    UUID schoolId, 
    UUID userId, 
    Instant startDate, 
    Instant endDate
) {
    
    public static SchoolUserDto toDto(SchoolUser sclUser) {
        return new SchoolUserDto(
            sclUser.getId(), 
            sclUser.getSchoolId(), 
            sclUser.getUserId(), 
            sclUser.getStartDate(), 
            sclUser.getEndDate()
        );
    }

    public static List<SchoolUserDto> toDtoList(List<SchoolUser> schoolUsers) {
        return schoolUsers.stream()
            .map(SchoolUserDto::toDto)
            .toList();
    }

    public static PageResult<SchoolUserDto> toDtoPage(PageResult<SchoolUser> schoolUserPage) {
        return PageResult.pageResult(
            toDtoList(schoolUserPage.content()),
            schoolUserPage.page(),
            schoolUserPage.size(),
            schoolUserPage.totalElements(),
            schoolUserPage.totalPages()
        );
    }
}
