package com.sep.vox.application.mapper.framework;

import java.util.UUID;

import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;

public final class CreateFrameworkVersionResponseMapper {

    public static CreateFrameworkVersionResponse toResponse(UUID versionId) {
        return new CreateFrameworkVersionResponse(versionId);
    }
}
