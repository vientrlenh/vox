package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolClassImportRequest(
    @NotEmpty(message = "Import confirmed mapping is required")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolClassImportCommand toCommand(UUID schoolId, UUID importSessionId, AcceptSchoolClassImportRequest request) {
        return new AcceptSchoolClassImportCommand(schoolId, importSessionId, request.confirmedMapping);
    }
}
