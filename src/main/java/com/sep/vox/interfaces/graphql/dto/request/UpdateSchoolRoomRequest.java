package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;


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
