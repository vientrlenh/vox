package com.sep.vox.interfaces.rest.dto.request.schoolroom;

import com.sep.vox.application.port.input.command.schoolroom.AcceptSchoolRoomImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolRoomImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolRoomImportCommand toCommand(UUID schoolId, UUID sessionId,
            AcceptSchoolRoomImportRequest request) {
        return new AcceptSchoolRoomImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
