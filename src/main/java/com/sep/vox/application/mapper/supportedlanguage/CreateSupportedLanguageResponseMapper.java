package com.sep.vox.application.mapper.supportedlanguage;

import java.util.UUID;

import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;

public final class CreateSupportedLanguageResponseMapper {

    private CreateSupportedLanguageResponseMapper() {
    }

    public static CreateSupportedLanguageResponse toResponse(UUID supportedLanguageId) {
        return new CreateSupportedLanguageResponse(supportedLanguageId);
    }
}
