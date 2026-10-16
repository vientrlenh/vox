package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolGradeImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolGradeImportRequest(
    @NotEmpty(message = "Confirmed mapping is required")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolGradeImportCommand toCommand(UUID schoolId, UUID importSessionId, AcceptSchoolGradeImportRequest request) {
        return new AcceptSchoolGradeImportCommand(schoolId, importSessionId, request.confirmedMapping);
    }
}
