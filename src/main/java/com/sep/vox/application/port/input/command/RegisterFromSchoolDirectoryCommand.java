package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterFromSchoolDirectoryCommand(
    UUID schoolDirectoryId,
    String contactFullName,
    String identityNumber,
    String contactPhone,
    String contactEmail,
    LocalDate dateOfBirth,
    String contactAddress,
    String postalCode,
    String position,
    int studentCount, 
    List<String> documentUrls
) {
    
}
