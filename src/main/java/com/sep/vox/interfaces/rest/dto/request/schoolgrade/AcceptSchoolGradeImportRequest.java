package com.sep.vox.interfaces.rest.dto.request.schoolgrade;

import com.sep.vox.application.port.input.command.schoolgrade.AcceptSchoolGradeImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolGradeImportRequest(
    @NotEmpty(message = "Import mapping must not be blank")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolGradeImportCommand toCommand(UUID schoolId, UUID sessionId, AcceptSchoolGradeImportRequest request) {
        return new AcceptSchoolGradeImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
