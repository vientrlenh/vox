package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkVersionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkVersionRequest;

public final class CreateFrameworkVersionCommandMapper {

    public static CreateFrameworkVersionCommand fromRequest(UUID frameworkId, CreateFrameworkVersionRequest request) {
        return new CreateFrameworkVersionCommand(
            frameworkId,
            request.code(),
            request.name(),
            request.description(),
            request.version(),
            request.effectiveFrom(),
            request.effectiveTo()
        );
    }
}
