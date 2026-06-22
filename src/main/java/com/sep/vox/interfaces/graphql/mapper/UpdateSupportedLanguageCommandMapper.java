package com.sep.vox.interfaces.graphql.mapper;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSupportedLanguageCommand;

public final class UpdateSupportedLanguageCommandMapper {

    private UpdateSupportedLanguageCommandMapper() {
    }

    public static UpdateSupportedLanguageCommand fromInput(UUID id, Map<String, Object> input) {
        return new UpdateSupportedLanguageCommand(
            id,
            valueOf(input.get("code")),
            input.containsKey("code"),
            valueOf(input.get("name")),
            input.containsKey("name"),
            valueOf(input.get("description")),
            input.containsKey("description"),
            booleanValueOf(input.get("isActive")),
            input.containsKey("isActive")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    private static Boolean booleanValueOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.valueOf(value.toString());
    }
}
