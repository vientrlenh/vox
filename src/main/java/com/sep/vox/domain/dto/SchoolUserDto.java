package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolUserDto(
    UUID id,
    String studentId, 
    UUID schoolId, 
    UUID userId, 
    String startDate, 
    String endDate
) {
    
}
