package com.sep.vox.interfaces.graphql.dto.request.gradelevel;

import com.sep.vox.application.port.input.command.gradelevel.UpdateGradeLevelCommand;
import java.util.UUID;

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
