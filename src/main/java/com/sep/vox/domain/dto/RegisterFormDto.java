package com.sep.vox.domain.dto;


import java.util.UUID;

public record RegisterFormDto(
    UUID id,
    UUID schoolDirectoryId, 
    String schoolName, 
    String schoolDomain, 
    String schoolDistrict, 
    String schoolProvince, 
    String schoolAddress, 
    String contactFullName, 
    String identityNumber, 
    String contactPhone, 
    String contactEmail, 
    String dateOfBirth, 
    String contactAddress, 
    String postalCode, 
    String position, 
    int studentCount, 
    String verificationMethod, 
    String rejectedReason,
    String status
) {
    
}
