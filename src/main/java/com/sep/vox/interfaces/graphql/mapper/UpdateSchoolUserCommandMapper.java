package com.sep.vox.interfaces.graphql.mapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolUserCommand;

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
            parseDateOfBirth(input.get("dateOfBirth")),
            input.containsKey("dateOfBirth")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDate parseDateOfBirth(Object value) {
        if (value == null) return null;
        return LocalDate.parse(value.toString());
    }
}
