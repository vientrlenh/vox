package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record SchoolUserDto(
    UUID id,
    UUID schoolId, 
    UUID userId, 
    Instant startDate, 
    Instant endDate
) {
    
}
