package com.sep.vox.application.response.input.supportedlanguage;

import java.util.UUID;

public record CreateSupportedLanguageResponse(
    UUID supportedLanguageId
) {

    public static CreateSupportedLanguageResponse toResponse(UUID supportedLanguageId) {
        return new CreateSupportedLanguageResponse(supportedLanguageId);
    }
}
