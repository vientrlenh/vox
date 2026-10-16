package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateGradeLevelCommand;

public record UpdateGradeLevelRequest(
        String name,
        String description,
        Integer order
) {

    public static UpdateGradeLevelCommand toCommand(UUID gradeLevelId, UpdateGradeLevelRequest request) {
        return new UpdateGradeLevelCommand(
                gradeLevelId,
                request.name(),
                request.description(),
                request.order()
        );
    }
}
