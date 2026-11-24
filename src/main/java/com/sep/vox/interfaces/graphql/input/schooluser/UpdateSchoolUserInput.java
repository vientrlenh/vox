package com.sep.vox.interfaces.graphql.input.schooluser;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;

public record UpdateSchoolUserInput(
        ArgumentValue<String> fullName,
        ArgumentValue<String> phone,
        ArgumentValue<String> address,
        ArgumentValue<LocalDate> dateOfBirth,
        ArgumentValue<String> avatarUrl) {

    public static UpdateSchoolUserCommand toCommand(UUID schoolId, UUID userId, UpdateSchoolUserInput input) {
        return new UpdateSchoolUserCommand(
                schoolId,
                userId,
                input.fullName().value(), !input.fullName().isOmitted(),
                input.phone().value(), !input.phone().isOmitted(),
                input.address().value(), !input.address().isOmitted(),
                input.dateOfBirth().value(), !input.dateOfBirth().isOmitted(),
                input.avatarUrl().value(), !input.avatarUrl().isOmitted());
    }
}
