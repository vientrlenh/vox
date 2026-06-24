package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSupportedLanguageRequest;

public final class CreateSupportedLanguageCommandMapper {

    private CreateSupportedLanguageCommandMapper() {
    }

    public static CreateSupportedLanguageCommand fromRequest(CreateSupportedLanguageRequest request) {
        return new CreateSupportedLanguageCommand(
            request.code(),
            request.name(),
            request.description()
        );
    }
}
