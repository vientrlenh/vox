package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolRoomRequest;

import java.util.UUID;

public class UpdateSchoolRoomCommandMapper {
    public static UpdateSchoolRoomCommand fromRequest(UUID roomId, UpdateSchoolRoomRequest request) {
        return new UpdateSchoolRoomCommand(
                roomId,
                request.name(),
                request.description(),
                request.capacity()
        );
    }
}
