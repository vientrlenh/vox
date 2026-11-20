package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;

public record SchoolDirectoryDto(
    UUID id, 
    String code, 
    String name, 
    String provinceCode, 
    String provinceName, 
    String districtName, 
    String domain, 
    String address, 
    SchoolDirectoryOrigin origin, 
    boolean verified, 
    Instant createdAt, 
    Instant updatedAt
) {
    
}
