package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AddSchoolRoomCommand;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;

import java.util.UUID;

public class AddSchoolRoomCommandMapper {
    public static AddSchoolRoomCommand fromRequest(UUID schoolId, AddSchoolRoomRequest request) {
        return new AddSchoolRoomCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.capacity()
        );
    }
}