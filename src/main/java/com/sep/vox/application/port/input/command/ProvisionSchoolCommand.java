package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProvisionSchoolCommand(
    String schoolCode, 
    String schoolName, 
    String description, 
    String schoolDomain, 
    String schoolAddress, 
    int studentCount, 
    String contactEmail, 
    String contactPhone, 
    String contactFullName, 
    LocalDate dateOfBirth, 
    String contactAddress, 
    String avatarUrl, 
    UUID createdUserId, 
    OffsetDateTime now
) {
    
}
