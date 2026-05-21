package com.sep.vox.application.command;

public record RegisterCommand(
    String contactFullName,
    String identityNumber,
    String contactPhone,
    String contactEmail,
    String schoolDomain,
    String schoolName,
    String schoolAddress,
    String postalCode,
    String position,
    int studentCount
) {
    
}
