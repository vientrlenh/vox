package com.sep.vox.application.response.input.framework;

import java.util.UUID;

public record CreateFrameworkVersionResponse(
    UUID frameworkVersionId
) {

    public static CreateFrameworkVersionResponse toResponse(UUID frameworkVersionId) {
        return new CreateFrameworkVersionResponse(frameworkVersionId);
    }
}
