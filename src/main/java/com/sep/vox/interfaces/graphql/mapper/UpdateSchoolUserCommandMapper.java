package com.sep.vox.interfaces.graphql.mapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;

public final class UpdateSchoolUserCommandMapper {

    private UpdateSchoolUserCommandMapper() {
    }

    public static UpdateSchoolUserCommand fromInput(UUID schoolId, UUID userId, Map<String, Object> input) {
        return new UpdateSchoolUserCommand(
            schoolId,
            userId,
            valueOf(input.get("fullName")),
            input.containsKey("fullName"),
            valueOf(input.get("phone")),
            input.containsKey("phone"),
            valueOf(input.get("address")),
            input.containsKey("address"),
            (LocalDate) input.get("dateOfBirth"),
            input.containsKey("dateOfBirth"),
            valueOf(input.get("avatarUrl")),
            input.containsKey("avatarUrl")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }
}
