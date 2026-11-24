package com.sep.vox.interfaces.rest.dto.request.schooluser;

import com.sep.vox.application.port.input.command.schooluser.AcceptSchoolUserImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolUserImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolUserImportCommand toCommand(UUID schoolId, UUID sessionId, AcceptSchoolUserImportRequest request) {
        return new AcceptSchoolUserImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
