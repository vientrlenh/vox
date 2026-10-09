package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolUser;

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
}
