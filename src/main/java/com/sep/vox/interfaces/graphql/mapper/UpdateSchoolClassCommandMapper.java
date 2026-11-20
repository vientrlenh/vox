package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;
import java.util.Map;

import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;

public final class UpdateSchoolClassCommandMapper {

    private UpdateSchoolClassCommandMapper() {
    }

    public static UpdateSchoolClassCommand fromInput(UUID id, Map<String, Object> input) {
        return new UpdateSchoolClassCommand(
            id,
            valueOf(input.get("name")),
            input.containsKey("name"),
            valueOf(input.get("description")),
            input.containsKey("description"),
            valueOf(input.get("status")),
            input.containsKey("status")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }
}
