package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterSchoolAdminCommand(
    String email,
    String phone,
    String fullName,
    LocalDate dateOfBirth,
    String address,
    UUID schoolId
) {
    
}
