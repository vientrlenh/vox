package com.sep.vox.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterFormStatus;

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
    LocalDate dateOfBirth, 
    String contactAddress, 
    String postalCode, 
    String position, 
    int studentCount, 
    String verificationMethod, 
    String rejectedReason,
    RegisterFormStatus status
) {
    
}
