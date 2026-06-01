package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AddSchoolRoomCommand;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;

public class AddSchoolRoomCommandMapper {
    public static AddSchoolRoomCommand fromRequest(AddSchoolRoomRequest request) {
        return new AddSchoolRoomCommand(
                request.schoolId(),
                request.code(),
                request.name(),
                request.description()
        );
    }
}