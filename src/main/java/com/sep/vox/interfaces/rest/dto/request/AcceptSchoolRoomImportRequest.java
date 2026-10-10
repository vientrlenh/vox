package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolRoomImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolRoomImportRequest(
    @NotEmpty(message = "Confirmed mapping is required")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolRoomImportCommand toCommand(UUID schoolId, UUID importSessionId, AcceptSchoolRoomImportRequest request) {
        return new AcceptSchoolRoomImportCommand(schoolId, importSessionId, request.confirmedMapping);
    }
}
