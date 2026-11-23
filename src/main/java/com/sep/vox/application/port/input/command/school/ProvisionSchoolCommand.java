package com.sep.vox.application.port.input.command.school;

import java.time.LocalDate;
import java.time.Instant;

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
    Instant now
) {
    
}
