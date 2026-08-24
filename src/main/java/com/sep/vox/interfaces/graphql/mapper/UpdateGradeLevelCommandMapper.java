package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateGradeLevelCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateGradeLevelRequest;

import java.util.UUID;

public final class UpdateGradeLevelCommandMapper {

    private UpdateGradeLevelCommandMapper() {
    }

    public static UpdateGradeLevelCommand fromRequest(UUID gradeLevelId, UpdateGradeLevelRequest request) {
        return new UpdateGradeLevelCommand(
                gradeLevelId,
                request.name(),
                request.description(),
                request.order()
        );
    }
}
