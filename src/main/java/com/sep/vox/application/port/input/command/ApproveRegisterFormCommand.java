package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record ApproveRegisterFormCommand(
    UUID registerFormId,
    String schoolCode,
    String schoolName,
    String description,
    String contactPhone,
    String contactEmail,
    String schoolDomain,
    String schoolAddress,
    int studentCount,
    String contactFullName,
    LocalDate dateOfBirth,
    String contactAddress
) {
    
}
