package com.sep.vox.interfaces.graphql.input.supportedlanguage;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.supportedlanguage.UpdateSupportedLanguageCommand;

public record UpdateSupportedLanguageInput(
        ArgumentValue<String> code,
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<Boolean> isActive) {

    public static UpdateSupportedLanguageCommand toCommand(UUID id, UpdateSupportedLanguageInput input) {
        return new UpdateSupportedLanguageCommand(
                id,
                input.code().value(), !input.code().isOmitted(),
                input.name().value(), !input.name().isOmitted(),
                input.description().value(), !input.description().isOmitted(),
                input.isActive().value(), !input.isActive().isOmitted());
    }
}
