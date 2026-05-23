package com.sep.vox.application.command;

import java.time.LocalDate;

public record RegisterCommand(
    String contactFullName,
    String identityNumber,
    String contactPhone,
    String contactEmail,
    LocalDate dateOfBirth,
    String contactAddress,
    String schoolDomain,
    String schoolName,
    String schoolAddress,
    String postalCode,
    String position,
    int studentCount
) {
    
}
