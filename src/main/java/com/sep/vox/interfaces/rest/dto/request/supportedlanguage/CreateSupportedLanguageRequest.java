package com.sep.vox.interfaces.rest.dto.request.supportedlanguage;

import com.sep.vox.application.port.input.command.supportedlanguage.CreateSupportedLanguageCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportedLanguageRequest(

    @NotBlank(message = "Language code must not be blank")
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    String code,

    @NotBlank(message = "Language name must not be blank")
    @Size(max = 100, message = "Language name must not exceed 100 characters")
    String name,

    @Size(max = 2048, message = "Language description must not exceed 2048 characters")
    String description
) {

    public static CreateSupportedLanguageCommand toCommand(CreateSupportedLanguageRequest request) {
        return new CreateSupportedLanguageCommand(
            request.code(),
            request.name(),
            request.description()
        );
    }
}
