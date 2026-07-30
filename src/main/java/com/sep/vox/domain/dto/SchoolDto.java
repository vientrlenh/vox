package com.sep.vox.domain.dto;

import java.time.Instant;
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
    Instant createdAt, 
    Instant updatedAt
) {
    
}
