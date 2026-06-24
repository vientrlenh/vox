package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolDirectoryDto(
    UUID id, 
    String code, 
    String name, 
    String provinceCode, 
    String provinceName, 
    String districtName, 
    String domain, 
    String address, 
    String origin, 
    boolean verified, 
    String createdAt, 
    String updatedAt
) {
    
}
