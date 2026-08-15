package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSchoolCommand(
    UUID schoolDirectoryId, 
    String schoolCode, 
    String schoolName, 
    String schoolAddress, 
    String schoolDomain, 
    int studentCount, 
    String adminEmail,
    String adminPhone,
    String adminFullName,
    LocalDate adminDateOfBirth,
    String adminAddress, 
    String adminAvatarUrl
) {
    
}
