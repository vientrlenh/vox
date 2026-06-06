package com.sep.vox.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolDto(
    UUID id,
    String code, 
    String name,
    String description,
    String contactPhone,
    String contactEmail,
    String domain,
    String address,
    int studentCount,
    boolean isActive, 
    OffsetDateTime createdAt, 
    OffsetDateTime updatedAt
) {
    
}
