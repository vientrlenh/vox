package com.sep.vox.interfaces.graphql.dto.request.schoolroom;

import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;
import java.util.UUID;

public record UpdateSchoolRoomRequest(
        String name,
        String description
) {

    public static UpdateSchoolRoomCommand toCommand(UUID id, UpdateSchoolRoomRequest request) {
        if (request == null) return null;

        return new UpdateSchoolRoomCommand(
                id,
                request.name(),
                request.description()
        );
    }
}
