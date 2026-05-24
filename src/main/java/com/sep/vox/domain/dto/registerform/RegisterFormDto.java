package com.sep.vox.domain.dto.registerform;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterFormDto(
    UUID id,
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
    int studentCount,
    String reason,
    String status
) {
    
}
