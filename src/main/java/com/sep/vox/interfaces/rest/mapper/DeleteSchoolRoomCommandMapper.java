package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.DeleteSchoolRoomCommand;

import java.util.UUID;

public class DeleteSchoolRoomCommandMapper {
    public static DeleteSchoolRoomCommand fromRequest(UUID id, UUID schoolId) {
        return new DeleteSchoolRoomCommand(
                schoolId,
                id);
    }
}
