package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolRoomImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolRoomImportRequest;

public final class AcceptSchoolRoomImportCommandMapper {

    private AcceptSchoolRoomImportCommandMapper() {
    }

    public static AcceptSchoolRoomImportCommand fromRequest(UUID schoolId, UUID sessionId,
            AcceptSchoolRoomImportRequest request) {
        return new AcceptSchoolRoomImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
