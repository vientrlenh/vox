package com.sep.vox.interfaces.rest.dto.request.schoolclass;

import com.sep.vox.application.port.input.command.schoolclass.AcceptSchoolClassImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolClassImportRequest(
    @NotEmpty(message = "Import mapping must not be blank")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolClassImportCommand toCommand(UUID schoolId, UUID sessionId, AcceptSchoolClassImportRequest request) {
        return new AcceptSchoolClassImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
