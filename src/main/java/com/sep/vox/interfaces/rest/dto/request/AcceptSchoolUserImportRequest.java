package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolUserImportRequest(
    @NotEmpty(message = "Confirmed mapping is required")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolUserImportCommand toCommand(UUID schoolId, UUID sessionId, AcceptSchoolUserImportRequest request) {
        return new AcceptSchoolUserImportCommand(schoolId, sessionId, request.confirmedMapping);
    }
}
