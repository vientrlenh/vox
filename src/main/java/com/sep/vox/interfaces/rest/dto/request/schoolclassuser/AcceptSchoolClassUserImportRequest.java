package com.sep.vox.interfaces.rest.dto.request.schoolclassuser;

import com.sep.vox.application.port.input.command.schoolclassuser.AcceptSchoolClassUserImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolClassUserImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolClassUserImportCommand toCommand(
            UUID schoolId,
            UUID sessionId,
            AcceptSchoolClassUserImportRequest request) {
        return new AcceptSchoolClassUserImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
