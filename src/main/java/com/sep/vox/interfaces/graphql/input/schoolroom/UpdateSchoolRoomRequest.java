package com.sep.vox.interfaces.graphql.input.schoolroom;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;

public record UpdateSchoolRoomRequest(
        ArgumentValue<String> name,
        ArgumentValue<String> description
) {

    public static UpdateSchoolRoomCommand toCommand(UUID id, UpdateSchoolRoomRequest request) {
        return new UpdateSchoolRoomCommand(
                id,
                request.name().value(),
                request.description().value(), !request.description().isOmitted()
        );
    }
}
