package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSchoolUserCommand(
        UUID schoolId,
        UUID userId,
        String fullName,
        boolean fullNameProvided,
        String phone,
        boolean phoneProvided,
        String address,
        boolean addressProvided,
        LocalDate dateOfBirth,
        boolean dateOfBirthProvided) {
}
