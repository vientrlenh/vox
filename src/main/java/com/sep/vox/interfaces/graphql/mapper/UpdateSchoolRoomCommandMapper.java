package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;

import java.util.UUID;

public class UpdateSchoolRoomCommandMapper {
    public static UpdateSchoolRoomCommand fromRequest(UUID id, UpdateSchoolRoomRequest request) {
        if (request == null) return null;

        return new UpdateSchoolRoomCommand(
                id,
                request.name(),
                request.description(),
                request.capacity()
        );
    }
}
